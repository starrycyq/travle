#!/bin/bash
cd /root/api_gateway
source venv/bin/activate
export $(cat .env | xargs)
python gateway.py