package com.travle.app.data.auth

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [34])
class AuthManagerTest {
    
    private lateinit var authManager: AuthManager
    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    private val testTokenInfo = TokenInfo(
        accessToken = "test_access_token",
        tokenType = "Bearer",
        expiresIn = 3600,
        issuedAt = System.currentTimeMillis()
    )
    
    private val testUser = AuthUser(
        userId = "test_user_123",
        username = "testuser",
        email = "test@example.com",
        role = "user"
    )
    
    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        
        mockContext = mockk()
        mockPrefs = mockk()
        mockEditor = mockk()
        
        every { mockContext.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE) } returns mockPrefs
        every { mockPrefs.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.putBoolean(any(), any()) } returns mockEditor
        every { mockEditor.remove(any()) } returns mockEditor
        every { mockEditor.apply() } just Runs
        
        authManager = AuthManager(mockContext)
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `saveLoginInfo should save token and user info`() = testScope.runTest {
        // Given
        every { mockPrefs.getString(any(), any()) } returns null
        
        // When
        authManager.saveLoginInfo(testTokenInfo, testUser)
        
        // Then
        verify {
            mockEditor.putString(
                "token_info",
                any<String>()
            )
            mockEditor.putString(
                "user_info",
                any<String>()
            )
            mockEditor.putBoolean("is_logged_in", true)
            mockEditor.apply()
        }
        
        assertEquals(AuthStatus.AUTHENTICATED, authManager.authStatus.value)
        assertEquals(testTokenInfo.accessToken, authManager.getCurrentToken())
        assertEquals(testUser, authManager.getCurrentUser())
    }
    
    @Test
    fun `saveLoginInfo should handle exception and set error status`() = testScope.runTest {
        // Given
        every { mockPrefs.edit() } throws RuntimeException("Test exception")
        
        // When
        try {
            authManager.saveLoginInfo(testTokenInfo, testUser)
            fail("Expected exception")
        } catch (e: Exception) {
            // Expected
        }
        
        // Then
        assertEquals(AuthStatus.ERROR, authManager.authStatus.value)
    }
    
    @Test
    fun `getCurrentToken should return token when authenticated`() = testScope.runTest {
        // Given
        authManager.saveLoginInfo(testTokenInfo, testUser)
        
        // When
        val token = authManager.getCurrentToken()
        
        // Then
        assertEquals(testTokenInfo.accessToken, token)
    }
    
    @Test
    fun `getCurrentUser should return user when authenticated`() = testScope.runTest {
        // Given
        authManager.saveLoginInfo(testTokenInfo, testUser)
        
        // When
        val user = authManager.getCurrentUser()
        
        // Then
        assertEquals(testUser, user)
    }
    
    @Test
    fun `isLoggedIn should return true when authenticated and token not expired`() = testScope.runTest {
        // Given
        authManager.saveLoginInfo(testTokenInfo, testUser)
        
        // When
        val isLoggedIn = authManager.isLoggedIn()
        
        // Then
        assertTrue(isLoggedIn)
    }
    
    @Test
    fun `isLoggedIn should return false when token expired`() = testScope.runTest {
        // Given
        val expiredToken = testTokenInfo.copy(
            issuedAt = System.currentTimeMillis() - 4000000 // 过期
        )
        authManager.saveLoginInfo(expiredToken, testUser)
        
        // When
        val isLoggedIn = authManager.isLoggedIn()
        
        // Then
        assertFalse(isLoggedIn)
    }
    
    @Test
    fun `isTokenExpired should return true for expired token`() = testScope.runTest {
        // Given
        val expiredToken = testTokenInfo.copy(
            issuedAt = System.currentTimeMillis() - 4000000 // 过期
        )
        authManager.saveLoginInfo(expiredToken, testUser)
        
        // When
        val isExpired = authManager.isTokenExpired()
        
        // Then
        assertTrue(isExpired)
    }
    
    @Test
    fun `isTokenExpired should return false for valid token`() = testScope.runTest {
        // Given
        authManager.saveLoginInfo(testTokenInfo, testUser)
        
        // When
        val isExpired = authManager.isTokenExpired()
        
        // Then
        assertFalse(isExpired)
    }
    
    @Test
    fun `updateToken should update token info`() = testScope.runTest {
        // Given
        authManager.saveLoginInfo(testTokenInfo, testUser)
        val newTokenInfo = testTokenInfo.copy(
            accessToken = "new_access_token",
            issuedAt = System.currentTimeMillis()
        )
        
        // When
        authManager.updateToken(newTokenInfo)
        
        // Then
        verify {
            mockEditor.putString(
                "token_info",
                any<String>()
            )
            mockEditor.apply()
        }
        
        assertEquals(newTokenInfo.accessToken, authManager.getCurrentToken())
        assertEquals(AuthStatus.AUTHENTICATED, authManager.authStatus.value)
    }
    
    @Test
    fun `logout should clear all auth data`() = testScope.runTest {
        // Given
        authManager.saveLoginInfo(testTokenInfo, testUser)
        
        // When
        authManager.logout()
        
        // Then
        verify {
            mockEditor.remove("token_info")
            mockEditor.remove("user_info")
            mockEditor.remove("is_logged_in")
            mockEditor.apply()
        }
        
        assertNull(authManager.getCurrentToken())
        assertNull(authManager.getCurrentUser())
        assertEquals(AuthStatus.UNAUTHENTICATED, authManager.authStatus.value)
    }
    
    @Test
    fun `checkTokenStatus should update status based on token expiration`() = testScope.runTest {
        // Given
        authManager.saveLoginInfo(testTokenInfo, testUser)
        
        // When
        authManager.checkTokenStatus()
        
        // Then
        assertEquals(AuthStatus.AUTHENTICATED, authManager.authStatus.value)
    }
    
    @Test
    fun `checkTokenStatus should set expired status for expired token`() = testScope.runTest {
        // Given
        val expiredToken = testTokenInfo.copy(
            issuedAt = System.currentTimeMillis() - 4000000
        )
        every { mockPrefs.getBoolean("is_logged_in", false) } returns true
        every { mockPrefs.getString("token_info", null) } returns Gson().toJson(expiredToken)
        every { mockPrefs.getString("user_info", null) } returns Gson().toJson(testUser)
        
        // Load auth data from preferences
        authManager = AuthManager(mockContext)
        
        // When
        testScope.runTest {
            authManager.checkTokenStatus()
        }
        
        // Then
        assertEquals(AuthStatus.EXPIRED, authManager.authStatus.value)
    }
    
    @Test
    fun `getAuthHeader should return Bearer token when authenticated`() = testScope.runTest {
        // Given
        authManager.saveLoginInfo(testTokenInfo, testUser)
        
        // When
        val authHeader = authManager.getAuthHeader()
        
        // Then
        assertEquals("Bearer ${testTokenInfo.accessToken}", authHeader)
    }
    
    @Test
    fun `getAuthHeader should return null when not authenticated`() = testScope.runTest {
        // When
        val authHeader = authManager.getAuthHeader()
        
        // Then
        assertNull(authHeader)
    }
    
    @Test
    fun `clearAll should logout user`() = testScope.runTest {
        // Given
        authManager.saveLoginInfo(testTokenInfo, testUser)
        
        // When
        authManager.clearAll()
        
        // Then
        assertEquals(AuthStatus.UNAUTHENTICATED, authManager.authStatus.value)
        assertNull(authManager.getCurrentToken())
        assertNull(authManager.getCurrentUser())
    }
    
    @Test
    fun `loadAuthData should load data from shared preferences`() {
        // Given
        every { mockPrefs.getBoolean("is_logged_in", false) } returns true
        every { mockPrefs.getString("token_info", null) } returns Gson().toJson(testTokenInfo)
        every { mockPrefs.getString("user_info", null) } returns Gson().toJson(testUser)
        
        // When
        authManager = AuthManager(mockContext)
        
        // Then
        assertEquals(testTokenInfo.accessToken, authManager.getCurrentToken())
        assertEquals(testUser, authManager.getCurrentUser())
        assertEquals(AuthStatus.AUTHENTICATED, authManager.authStatus.value)
    }
    
    @Test
    fun `loadAuthData should set unauthenticated when no saved data`() {
        // Given
        every { mockPrefs.getBoolean("is_logged_in", false) } returns false
        
        // When
        authManager = AuthManager(mockContext)
        
        // Then
        assertEquals(AuthStatus.UNAUTHENTICATED, authManager.authStatus.value)
        assertNull(authManager.getCurrentToken())
        assertNull(authManager.getCurrentUser())
    }
    
    @Test
    fun `loadAuthData should set expired status for expired token`() {
        // Given
        val expiredToken = testTokenInfo.copy(
            issuedAt = System.currentTimeMillis() - 4000000
        )
        every { mockPrefs.getBoolean("is_logged_in", false) } returns true
        every { mockPrefs.getString("token_info", null) } returns Gson().toJson(expiredToken)
        every { mockPrefs.getString("user_info", null) } returns Gson().toJson(testUser)
        
        // When
        authManager = AuthManager(mockContext)
        
        // Then
        assertEquals(AuthStatus.EXPIRED, authManager.authStatus.value)
        assertEquals(expiredToken.accessToken, authManager.getCurrentToken())
        assertEquals(testUser, authManager.getCurrentUser())
    }
    
    @Test
    fun `loadAuthData should handle json parsing error`() {
        // Given
        every { mockPrefs.getBoolean("is_logged_in", false) } returns true
        every { mockPrefs.getString("token_info", null) } returns "invalid_json"
        every { mockPrefs.getString("user_info", null) } returns "invalid_json"
        
        // When
        authManager = AuthManager(mockContext)
        
        // Then
        assertEquals(AuthStatus.ERROR, authManager.authStatus.value)
    }
}