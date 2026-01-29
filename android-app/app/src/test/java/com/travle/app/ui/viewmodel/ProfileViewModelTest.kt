package com.travle.app.ui.viewmodel

import com.travle.app.data.repository.PreferencesRepository
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
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
class ProfileViewModelTest {
    
    private lateinit var viewModel: ProfileViewModel
    private lateinit var mockPreferencesRepository: PreferencesRepository
    
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    private val testNicknameFlow = MutableStateFlow("初始昵称")
    private val testThemeFlow = MutableStateFlow("light")
    
    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        
        mockPreferencesRepository = mockk()
        every { mockPreferencesRepository.nickname } returns testNicknameFlow
        every { mockPreferencesRepository.theme } returns testThemeFlow
        coEvery { mockPreferencesRepository.setNickname(any()) } just Runs
        coEvery { mockPreferencesRepository.setTheme(any()) } just Runs
        
        viewModel = ProfileViewModel(mockPreferencesRepository)
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `viewModel should initialize with default state`() = testScope.runTest {
        // Given
        val expectedState = ProfileUiState(
            nickname = "初始昵称",
            theme = "light",
            collectionsCount = 12,
            sharesCount = 8,
            likesCount = 156
        )
        
        // When
        val initialState = viewModel.uiState.value
        
        // Then
        assertEquals(expectedState, initialState)
    }
    
    @Test
    fun `viewModel should update nickname when repository nickname changes`() = testScope.runTest {
        // Given
        val newNickname = "新昵称"
        val expectedState = ProfileUiState(
            nickname = newNickname,
            theme = "light",
            collectionsCount = 12,
            sharesCount = 8,
            likesCount = 156
        )
        
        // When
        testNicknameFlow.value = newNickname
        
        // Wait for flow collection
        testScope.advanceUntilIdle()
        
        // Then
        val updatedState = viewModel.uiState.value
        assertEquals(expectedState, updatedState)
    }
    
    @Test
    fun `viewModel should update theme when repository theme changes`() = testScope.runTest {
        // Given
        val newTheme = "dark"
        val expectedState = ProfileUiState(
            nickname = "初始昵称",
            theme = newTheme,
            collectionsCount = 12,
            sharesCount = 8,
            likesCount = 156
        )
        
        // When
        testThemeFlow.value = newTheme
        
        // Wait for flow collection
        testScope.advanceUntilIdle()
        
        // Then
        val updatedState = viewModel.uiState.value
        assertEquals(expectedState, updatedState)
    }
    
    @Test
    fun `updateNickname should call repository setNickname`() = testScope.runTest {
        // Given
        val newNickname = "测试昵称"
        
        // When
        viewModel.updateNickname(newNickname)
        
        // Then
        coVerify { mockPreferencesRepository.setNickname(newNickname) }
    }
    
    @Test
    fun `updateTheme should call repository setTheme`() = testScope.runTest {
        // Given
        val newTheme = "dark"
        
        // When
        viewModel.updateTheme(newTheme)
        
        // Then
        coVerify { mockPreferencesRepository.setTheme(newTheme) }
    }
    
    @Test
    fun `viewModel should handle multiple state updates correctly`() = testScope.runTest {
        // Given
        val newNickname = "昵称1"
        val newTheme = "dark"
        
        // When
        testNicknameFlow.value = newNickname
        testThemeFlow.value = newTheme
        
        testScope.advanceUntilIdle()
        
        // Then
        val expectedState = ProfileUiState(
            nickname = newNickname,
            theme = newTheme,
            collectionsCount = 12,
            sharesCount = 8,
            likesCount = 156
        )
        
        assertEquals(expectedState, viewModel.uiState.value)
        
        // Update nickname via repository call
        val anotherNickname = "昵称2"
        viewModel.updateNickname(anotherNickname)
        
        coVerify { mockPreferencesRepository.setNickname(anotherNickname) }
        
        // Simulate repository updating the flow
        testNicknameFlow.value = anotherNickname
        testScope.advanceUntilIdle()
        
        val finalExpectedState = ProfileUiState(
            nickname = anotherNickname,
            theme = newTheme,
            collectionsCount = 12,
            sharesCount = 8,
            likesCount = 156
        )
        
        assertEquals(finalExpectedState, viewModel.uiState.value)
    }
    
    @Test
    fun `uiState should emit correct values over time`() = testScope.runTest {
        // Given
        val states = mutableListOf<ProfileUiState>()
        val job = testScope.launch {
            viewModel.uiState.collect { states.add(it) }
        }
        
        // When - change nickname
        testNicknameFlow.value = "昵称A"
        testScope.advanceUntilIdle()
        
        // When - change theme
        testThemeFlow.value = "dark"
        testScope.advanceUntilIdle()
        
        // When - change nickname again
        testNicknameFlow.value = "昵称B"
        testScope.advanceUntilIdle()
        
        job.cancel()
        
        // Then
        assertEquals(4, states.size) // Initial + 3 updates
        
        assertEquals("初始昵称", states[0].nickname)
        assertEquals("light", states[0].theme)
        
        assertEquals("昵称A", states[1].nickname)
        assertEquals("light", states[1].theme)
        
        assertEquals("昵称A", states[2].nickname)
        assertEquals("dark", states[2].theme)
        
        assertEquals("昵称B", states[3].nickname)
        assertEquals("dark", states[3].theme)
    }
    
    @Test
    fun `viewModel should maintain other state fields when updating nickname or theme`() = testScope.runTest {
        // Given
        val initialState = viewModel.uiState.value
        
        // When
        testNicknameFlow.value = "新昵称"
        testScope.advanceUntilIdle()
        
        // Then
        val updatedState = viewModel.uiState.value
        assertEquals("新昵称", updatedState.nickname)
        assertEquals(initialState.theme, updatedState.theme)
        assertEquals(initialState.collectionsCount, updatedState.collectionsCount)
        assertEquals(initialState.sharesCount, updatedState.sharesCount)
        assertEquals(initialState.likesCount, updatedState.likesCount)
    }
}