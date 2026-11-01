package com.lihan.localvault.feature.vault.presentation.create

import com.lihan.localvault.core.domain.Vault
import com.lihan.localvault.core.domain.VaultContent
import com.lihan.localvault.core.domain.datastore.AppDataStore
import com.lihan.localvault.core.domain.util.DataError
import com.lihan.localvault.core.domain.util.EmptyResult
import com.lihan.localvault.core.domain.util.Result
import com.lihan.localvault.core.data.security.storage.FakeEncryptedFileManager
import com.lihan.localvault.feature.vault.domain.VaultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateVaultViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeFileManager = FakeEncryptedFileManager()

    private val fakeRepo = object : VaultRepository {
        override val vaultItems: Flow<List<Vault>> = emptyFlow()
        override suspend fun getVaultById(id: Long): Result<Vault, DataError.Local> = Result.Error(DataError.Local.NOT_FOUND)
        override suspend fun getVaultContent(id: Long): Result<VaultContent, DataError.Local> = Result.Error(DataError.Local.NOT_FOUND)
        override suspend fun deleteVault(id: Long): EmptyResult<DataError.Local> = Result.Success(Unit)
        override suspend fun upsertVault(vault: Vault, content: VaultContent): EmptyResult<DataError.Local> = Result.Success(Unit)
        override suspend fun exportBackup(passphrase: String, includeAttachments: Boolean): Result<String, DataError.Local> = Result.Success("")
        override suspend fun importBackup(passphrase: String, backupJson: String): Result<Int, DataError.Local> = Result.Success(0)
    }

    private val fakeDataStore = object : AppDataStore {
        override val lastUsedColor: Flow<Long?> = flowOf(null)
        override val lastUsedIconIndex: Flow<Int?> = flowOf(null)
        override val isDarkTheme: Flow<Boolean> = flowOf(true)
        override val isLimitCardCollapsed: Flow<Boolean> = flowOf(false)
        override suspend fun saveLastUsedVaultStyle(color: Long, iconIndex: Int) {}
        override suspend fun setDarkTheme(isDark: Boolean) {}
        override suspend fun toggleTheme() {}
        override suspend fun setLimitCardCollapsed(isCollapsed: Boolean) {}
        override suspend fun toggleLimitCardCollapsed() {}
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onTakePhotoClick_closesMenuAndEmitsLaunchCameraEvent() = runTest {
        val viewModel = CreateVaultViewModel(fakeRepo, fakeDataStore, fakeFileManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }
        val emittedEvents = mutableListOf<CreateVaultEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(emittedEvents)
        }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAction(CreateVaultAction.OnOpenAddAttachmentMenu)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.isAddAttachmentMenuOpen)

        viewModel.onAction(CreateVaultAction.OnTakePhotoClick)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isAddAttachmentMenuOpen)
        assertEquals(1, emittedEvents.size)
        assertTrue(emittedEvents.first() is CreateVaultEvent.LaunchCamera)
    }

    @Test
    fun onChoosePhotoClick_closesMenuAndEmitsLaunchPhotoPickerEvent() = runTest {
        val viewModel = CreateVaultViewModel(fakeRepo, fakeDataStore, fakeFileManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }
        val emittedEvents = mutableListOf<CreateVaultEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(emittedEvents)
        }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAction(CreateVaultAction.OnOpenAddAttachmentMenu)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.isAddAttachmentMenuOpen)

        viewModel.onAction(CreateVaultAction.OnChoosePhotoClick)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isAddAttachmentMenuOpen)
        assertEquals(1, emittedEvents.size)
        assertTrue(emittedEvents.first() is CreateVaultEvent.LaunchPhotoPicker)
    }

    @Test
    fun onChooseDocumentClick_closesMenuAndEmitsLaunchDocumentPickerEvent() = runTest {
        val viewModel = CreateVaultViewModel(fakeRepo, fakeDataStore, fakeFileManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }
        val emittedEvents = mutableListOf<CreateVaultEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(emittedEvents)
        }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAction(CreateVaultAction.OnOpenAddAttachmentMenu)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.isAddAttachmentMenuOpen)

        viewModel.onAction(CreateVaultAction.OnChooseDocumentClick)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isAddAttachmentMenuOpen)
        assertEquals(1, emittedEvents.size)
        assertTrue(emittedEvents.first() is CreateVaultEvent.LaunchDocumentPicker)
    }
}
