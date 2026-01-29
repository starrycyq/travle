"""
数据库优化工具
提供数据库索引优化、查询分析和性能调优功能
"""

import sqlite3
import logging
from typing import List, Dict, Any
from datetime import datetime

logger = logging.getLogger(__name__)

class DatabaseOptimizer:
    """数据库优化器"""
    
    def __init__(self, db_path: str = None):
        self.db_path = db_path or './preferences.db'
        self.conn = None
    
    def connect(self):
        """连接到数据库"""
        self.conn = sqlite3.connect(self.db_path)
        self.conn.row_factory = sqlite3.Row
        return self.conn
    
    def close(self):
        """关闭数据库连接"""
        if self.conn:
            self.conn.close()
            self.conn = None
    
    def analyze_tables(self) -> List[Dict[str, Any]]:
        """分析数据库表结构"""
        self.connect()
        cursor = self.conn.cursor()
        
        tables = []
        cursor.execute("SELECT name FROM sqlite_master WHERE type='table'")
        table_names = [row[0] for row in cursor.fetchall()]
        
        for table_name in table_names:
            # 获取表信息
            cursor.execute(f"PRAGMA table_info({table_name})")
            columns = cursor.fetchall()
            
            # 获取索引信息
            cursor.execute(f"PRAGMA index_list({table_name})")
            indexes = cursor.fetchall()
            
            # 获取表大小
            cursor.execute(f"SELECT COUNT(*) as row_count FROM {table_name}")
            row_count = cursor.fetchone()[0]
            
            tables.append({
                'name': table_name,
                'columns': [{'cid': col[0], 'name': col[1], 'type': col[2], 
                            'notnull': col[3], 'default': col[4], 'pk': col[5]} 
                           for col in columns],
                'indexes': indexes,
                'row_count': row_count
            })
        
        self.close()
        return tables
    
    def get_recommended_indexes(self) -> List[Dict[str, str]]:
        """获取推荐的索引"""
        recommendations = []
        
        # 分析查询模式并推荐索引
        # 1. preferences表查询模式
        recommendations.extend([
            {
                'table': 'preferences',
                'column': 'destination',
                'index_name': 'idx_preferences_destination',
                'reason': '按目的地查询用户偏好',
                'query_example': 'SELECT * FROM preferences WHERE destination = ?'
            },
            {
                'table': 'preferences',
                'column': 'timestamp',
                'index_name': 'idx_preferences_timestamp',
                'reason': '按时间范围查询偏好记录',
                'query_example': 'SELECT * FROM preferences WHERE timestamp BETWEEN ? AND ?'
            }
        ])
        
        # 2. community_post表查询模式
        recommendations.extend([
            {
                'table': 'community_post',
                'column': 'create_time',
                'index_name': 'idx_community_post_create_time',
                'reason': '按时间排序社区动态',
                'query_example': 'SELECT * FROM community_post ORDER BY create_time DESC'
            },
            {
                'table': 'community_post',
                'column': 'destination',
                'index_name': 'idx_community_post_destination',
                'reason': '按目的地筛选社区动态',
                'query_example': 'SELECT * FROM community_post WHERE destination = ?'
            },
            {
                'table': 'community_post',
                'column': 'like_count',
                'index_name': 'idx_community_post_like_count',
                'reason': '按点赞数排序热门动态',
                'query_example': 'SELECT * FROM community_post ORDER BY like_count DESC'
            }
        ])
        
        # 3. community_comment表查询模式
        recommendations.extend([
            {
                'table': 'community_comment',
                'column': 'post_id',
                'index_name': 'idx_community_comment_post_id',
                'reason': '按帖子ID查询评论',
                'query_example': 'SELECT * FROM community_comment WHERE post_id = ?'
            },
            {
                'table': 'community_comment',
                'column': 'create_time',
                'index_name': 'idx_community_comment_create_time',
                'reason': '按时间排序评论',
                'query_example': 'SELECT * FROM community_comment WHERE post_id = ? ORDER BY create_time'
            }
        ])
        
        return recommendations
    
    def create_indexes(self, indexes: List[Dict[str, str]] = None) -> Dict[str, Any]:
        """创建索引"""
        if indexes is None:
            indexes = self.get_recommended_indexes()
        
        self.connect()
        cursor = self.conn.cursor()
        
        results = {
            'created': [],
            'skipped': [],
            'errors': []
        }
        
        for index_info in indexes:
            table = index_info['table']
            column = index_info['column']
            index_name = index_info['index_name']
            
            try:
                # 检查索引是否已存在
                cursor.execute(f"SELECT name FROM sqlite_master WHERE type='index' AND name='{index_name}'")
                if cursor.fetchone():
                    logger.info(f"索引已存在: {index_name}")
                    results['skipped'].append(index_name)
                    continue
                
                # 创建索引
                create_sql = f"CREATE INDEX {index_name} ON {table}({column})"
                cursor.execute(create_sql)
                logger.info(f"创建索引: {index_name} ON {table}({column})")
                results['created'].append(index_name)
                
            except Exception as e:
                error_msg = f"创建索引失败 {index_name}: {e}"
                logger.error(error_msg)
                results['errors'].append(error_msg)
        
        self.conn.commit()
        self.close()
        
        return results
    
    def analyze_query_performance(self, query: str, params: tuple = None) -> Dict[str, Any]:
        """分析查询性能"""
        self.connect()
        cursor = self.conn.cursor()
        
        # 启用SQLite的查询计划
        cursor.execute("EXPLAIN QUERY PLAN " + query, params or ())
        query_plan = cursor.fetchall()
        
        # 执行查询获取实际性能
        import time
        start_time = time.perf_counter()
        cursor.execute(query, params or ())
        results = cursor.fetchall()
        execution_time = time.perf_counter() - start_time
        
        # 获取查询统计信息
        row_count = len(results)
        
        analysis = {
            'query': query,
            'params': params,
            'execution_time': execution_time,
            'row_count': row_count,
            'query_plan': query_plan,
            'suggestions': []
        }
        
        # 分析查询计划并提供建议
        plan_text = ' '.join(str(row) for row in query_plan)
        
        if 'SCAN TABLE' in plan_text and 'USING INDEX' not in plan_text:
            analysis['suggestions'].append(
                "建议: 查询执行全表扫描，考虑添加索引"
            )
        
        if 'TEMPORARY B-TREE' in plan_text:
            analysis['suggestions'].append(
                "建议: 查询创建临时索引，考虑添加合适的索引"
            )
        
        if execution_time > 0.1:  # 超过100ms
            analysis['suggestions'].append(
                f"警告: 查询执行时间较长 ({execution_time:.3f}s)，建议优化"
            )
        
        self.close()
        return analysis
    
    def optimize_database(self):
        """执行完整的数据库优化"""
        logger.info("开始数据库优化")
        
        # 1. 分析当前表结构
        tables = self.analyze_tables()
        logger.info(f"分析完成，共 {len(tables)} 个表")
        
        # 2. 创建推荐的索引
        recommendations = self.get_recommended_indexes()
        index_results = self.create_indexes(recommendations)
        
        # 3. 分析关键查询性能
        key_queries = [
            ("SELECT * FROM preferences WHERE destination = ?", ('北京',)),
            ("SELECT * FROM community_post ORDER BY create_time DESC LIMIT 20", ()),
            ("SELECT * FROM community_comment WHERE post_id = ? ORDER BY create_time", (1,)),
            ("SELECT * FROM community_post WHERE destination = ? ORDER BY like_count DESC", ('上海',))
        ]
        
        query_analyses = []
        for query, params in key_queries:
            analysis = self.analyze_query_performance(query, params)
            query_analyses.append(analysis)
        
        # 4. 生成优化报告
        report = self.generate_optimization_report(
            tables, index_results, query_analyses
        )
        
        logger.info("数据库优化完成")
        return report
    
    def generate_optimization_report(self, tables, index_results, query_analyses):
        """生成优化报告"""
        report_lines = []
        report_lines.append("=" * 80)
        report_lines.append("数据库优化报告")
        report_lines.append("=" * 80)
        report_lines.append(f"生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        report_lines.append(f"数据库: {self.db_path}")
        report_lines.append("")
        
        # 表结构分析
        report_lines.append("📊 表结构分析")
        for table in tables:
            report_lines.append(f"  表: {table['name']}")
            report_lines.append(f"    行数: {table['row_count']:,}")
            report_lines.append(f"    列数: {len(table['columns'])}")
            report_lines.append(f"    索引数: {len(table['indexes'])}")
        report_lines.append("")
        
        # 索引优化结果
        report_lines.append("🔧 索引优化")
        report_lines.append(f"  创建索引: {len(index_results['created'])} 个")
        if index_results['created']:
            report_lines.append("    已创建:")
            for idx in index_results['created']:
                report_lines.append(f"    • {idx}")
        
        report_lines.append(f"  跳过索引: {len(index_results['skipped'])} 个")
        report_lines.append(f"  错误: {len(index_results['errors'])} 个")
        if index_results['errors']:
            report_lines.append("    错误详情:")
            for error in index_results['errors']:
                report_lines.append(f"    • {error}")
        report_lines.append("")
        
        # 查询性能分析
        report_lines.append("⚡ 查询性能分析")
        for analysis in query_analyses:
            report_lines.append(f"  查询: {analysis['query'][:60]}...")
            report_lines.append(f"    执行时间: {analysis['execution_time']:.3f}s")
            report_lines.append(f"    返回行数: {analysis['row_count']}")
            
            if analysis['suggestions']:
                report_lines.append("    优化建议:")
                for suggestion in analysis['suggestions']:
                    report_lines.append(f"    • {suggestion}")
            else:
                report_lines.append("    ✅ 查询性能良好")
            report_lines.append("")
        
        # 总体建议
        report_lines.append("💡 总体优化建议")
        report_lines.append("  1. 定期运行数据库优化（建议每周一次）")
        report_lines.append("  2. 监控慢查询日志")
        report_lines.append("  3. 考虑对大表进行分区")
        report_lines.append("  4. 定期清理过期数据")
        report_lines.append("  5. 启用数据库连接池")
        
        return "\n".join(report_lines)
    
    def vacuum_database(self):
        """执行数据库VACUUM操作（压缩和重建数据库）"""
        logger.info("开始执行VACUUM操作")
        self.connect()
        cursor = self.conn.cursor()
        
        try:
            cursor.execute("VACUUM")
            logger.info("VACUUM操作完成")
            return True
        except Exception as e:
            logger.error(f"VACUUM操作失败: {e}")
            return False
        finally:
            self.close()

def main():
    """主函数"""
    import argparse
    
    parser = argparse.ArgumentParser(description='数据库优化工具')
    parser.add_argument('--db-path', default='./preferences.db', help='数据库文件路径')
    parser.add_argument('--optimize', action='store_true', help='执行完整优化')
    parser.add_argument('--analyze', action='store_true', help='分析数据库')
    parser.add_argument('--create-indexes', action='store_true', help='创建推荐索引')
    parser.add_argument('--vacuum', action='store_true', help='执行VACUUM操作')
    parser.add_argument('--output', help='输出报告文件路径')
    
    args = parser.parse_args()
    
    # 配置日志
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )
    
    optimizer = DatabaseOptimizer(args.db_path)
    
    if args.analyze:
        tables = optimizer.analyze_tables()
        print(f"分析完成，共 {len(tables)} 个表")
        for table in tables:
            print(f"\n表: {table['name']}")
            print(f"  行数: {table['row_count']:,}")
            print(f"  列数: {len(table['columns'])}")
            print(f"  索引数: {len(table['indexes'])}")
    
    if args.create_indexes:
        recommendations = optimizer.get_recommended_indexes()
        print(f"将创建 {len(recommendations)} 个索引")
        results = optimizer.create_indexes(recommendations)
        print(f"创建完成: {len(results['created'])} 成功, {len(results['skipped'])} 跳过, {len(results['errors'])} 错误")
    
    if args.vacuum:
        optimizer.vacuum_database()
    
    if args.optimize:
        report = optimizer.optimize_database()
        print("\n" + report)
        
        if args.output:
            with open(args.output, 'w', encoding='utf-8') as f:
                f.write(report)
            print(f"报告已保存到: {args.output}")

if __name__ == '__main__':
    main()