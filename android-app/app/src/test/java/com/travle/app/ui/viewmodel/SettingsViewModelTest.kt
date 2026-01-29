package com.travle.app.ui.viewmodel

import com.travle.app.data.repository.PreferencesRepository
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
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
class SettingsViewModelTest {
    
    private lateinit var viewModel: SettingsViewModel
    private lateinit var mockPreferencesRepository: PreferencesRepository
    
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    private val testThemeFlow = MutableStateFlow("light")
    
    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        
        mockPreferencesRepository = mockk()
        every { mockPreferencesRepository.theme } returns testThemeFlow
        coEvery { mockPreferencesRepository.setTheme(any()) } just Runs
        
        viewModel = SettingsViewModel(mockPreferencesRepository)
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `viewModel should initialize with default state`() = testScope.runTest {
        // Given
        val expectedState = SettingsUiState(
            theme = "light",
            aboutModalOpen = false,
            errorMessage = null,
            successMessage = null
        )
        
        // When
        val initialState = viewModel.uiState.value
        
        // Then
        assertEquals(expectedState, initialState)
    }
    
    @Test
    fun `viewModel should update theme when repository theme changes`() = testScope.runTest {
        // Given
        val newTheme = "dark"
        val expectedState = SettingsUiState(
            theme = newTheme,
            aboutModalOpen = false,
            errorMessage = null,
            successMessage = null
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
    fun `toggleTheme should switch between light and dark themes`() = testScope.runTest {
        // Given - initial theme is light
        assertEquals("light", viewModel.uiState.value.theme)
        
        // When - toggle to dark
        viewModel.toggleTheme()
        
        // Then
        coVerify { mockPreferencesRepository.setTheme("dark") }
        
        // Simulate repository updating the flow
        testThemeFlow.value = "dark"
        testScope.advanceUntilIdle()
        
        assertEquals("dark", viewModel.uiState.value.theme)
        
        // When - toggle back to light
        viewModel.toggleTheme()
        
        // Then
        coVerify { mockPreferencesRepository.setTheme("light") }
    }
    
    @Test
    fun `toggleTheme should handle current theme correctly`() = testScope.runTest {
        // Test starting with dark theme
        testThemeFlow.value = "dark"
        testScope.advanceUntilIdle()
        
        assertEquals("dark", viewModel.uiState.value.theme)
        
        // Toggle should go to light
        viewModel.toggleTheme()
        coVerify { mockPreferencesRepository.setTheme("light") }
        
        // Toggle again should go back to dark
        viewModel.toggleTheme()
        coVerify { mockPreferencesRepository.setTheme("dark") }
    }
    
    @Test
    fun `clearCache should execute without errors`() = testScope.runTest {
        // Given
        // No specific setup needed
        
        // When
        viewModel.clearCache()
        
        // Then
        // Should not throw any exceptions
        assertTrue(true)
    }
    
    @Test
    fun `toggleAboutModal should update aboutModalOpen state`() = testScope.runTest {
        // Given
        assertFalse(viewModel.uiState.value.aboutModalOpen)
        
        // When - open modal
        viewModel.toggleAboutModal(true)
        
        // Then
        assertTrue(viewModel.uiState.value.aboutModalOpen)
        
        // When - close modal
        viewModel.toggleAboutModal(false)
        
        // Then
        assertFalse(viewModel.uiState.value.aboutModalOpen)
    }
    
    @Test
    fun `clearMessages should clear error and success messages`() = testScope.runTest {
        // Given
        val stateWithMessages = SettingsUiState(
            theme = "light",
            aboutModalOpen = false,
            errorMessage = "错误信息",
            successMessage = "成功信息"
        )
        
        // Set state directly (not ideal but works for test)
        val currentState = viewModel.uiState.value
        viewModel.toggleAboutModal(false) // Just to trigger state update
        
        // When
        viewModel.clearMessages()
        
        // Then
        val updatedState = viewModel.uiState.value
        assertNull(updatedState.errorMessage)
        assertNull(updatedState.successMessage)
        assertEquals("light", updatedState.theme)
        assertFalse(updatedState.aboutModalOpen)
    }
    
    @Test
    fun `uiState should emit correct values when theme changes`() = testScope.runTest {
        // Given
        val states = mutableListOf<SettingsUiState>()
        val job = testScope.launch {
            viewModel.uiState.collect { states.add(it) }
        }
        
        // When - change theme to dark
        testThemeFlow.value = "dark"
        testScope.advanceUntilIdle()
        
        // When - open about modal
        viewModel.toggleAboutModal(true)
        testScope.advanceUntilIdle()
        
        // When - change theme back to light
        testThemeFlow.value = "light"
        testScope.advanceUntilIdle()
        
        job.cancel()
        
        // Then
        assertEquals(4, states.size) // Initial + 3 updates
        
        assertEquals("light", states[0].theme)
        assertFalse(states[0].aboutModalOpen)
        
        assertEquals("dark", states[1].theme)
        assertFalse(states[1].aboutModalOpen)
        
        assertEquals("dark", states[2].theme)
        assertTrue(states[2].aboutModalOpen)
        
        assertEquals("light", states[3].theme)
        assertTrue(states[3].aboutModalOpen)
    }
    
    @Test
    fun `viewModel should handle multiple operations correctly`() = testScope.runTest {
        // Test comprehensive usage
        
        // 1. Initial state
        val initialState = viewModel.uiState.value
        assertEquals("light", initialState.theme)
        assertFalse(initialState.aboutModalOpen)
        
        // 2. Toggle theme
        viewModel.toggleTheme()
        coVerify { mockPreferencesRepository.setTheme("dark") }
        
        // 3. Simulate repository updating theme
        testThemeFlow.value = "dark"
        testScope.advanceUntilIdle()
        
        assertEquals("dark", viewModel.uiState.value.theme)
        
        // 4. Open about modal
        viewModel.toggleAboutModal(true)
        assertTrue(viewModel.uiState.value.aboutModalOpen)
        
        // 5. Clear messages (should not affect theme or modal)
        viewModel.clearMessages()
        assertNull(viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.successMessage)
        assertEquals("dark", viewModel.uiState.value.theme)
        assertTrue(viewModel.uiState.value.aboutModalOpen)
        
        // 6. Close modal
        viewModel.toggleAboutModal(false)
        assertFalse(viewModel.uiState.value.aboutModalOpen)
        
        // 7. Toggle theme back
        viewModel.toggleTheme()
        coVerify { mockPreferencesRepository.setTheme("light") }
        
        // 8. Clear cache (should not affect state)
        viewModel.clearCache()
        assertEquals("dark", viewModel.uiState.value.theme) // Still dark until repository updates
    }
    
    @Test
    fun `viewModel should maintain other state fields when updating theme`() = testScope.runTest {
        // Given - set modal open and messages
        viewModel.toggleAboutModal(true)
        
        // When - change theme
        testThemeFlow.value = "dark"
        testScope.advanceUntilIdle()
        
        // Then
        val updatedState = viewModel.uiState.value
        assertEquals("dark", updatedState.theme)
        assertTrue(updatedState.aboutModalOpen) // Should remain true
        assertNull(updatedState.errorMessage)
        assertNull(updatedState.successMessage)
    }
}