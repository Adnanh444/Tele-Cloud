package com.adnan.memoracloud.core.transfer

import com.adnan.memoracloud.core.crypto.StreamCrypto
import com.adnan.memoracloud.core.dispatchers.DispatcherProvider
import com.adnan.memoracloud.core.files.AppStoragePaths
import com.adnan.memoracloud.core.media.MediaPart
import com.adnan.memoracloud.core.media.ThumbnailStore
import com.adnan.memoracloud.core.telegram.TelegramClient
import com.adnan.memoracloud.data.local.entity.FileEntity
import com.adnan.memoracloud.data.local.entity.FilePartEntity
import com.adnan.memoracloud.data.local.entity.TransferEntity
import com.adnan.memoracloud.data.remote.telegram.ManifestCodec
import com.adnan.memoracloud.data.repository.ActiveChannel
import com.adnan.memoracloud.data.repository.FakeSettings
import com.adnan.memoracloud.data.repository.FolderPathResolver
import com.adnan.memoracloud.data.repository.SyncHarness
import com.adnan.memoracloud.domain.model.BackupState
import com.adnan.memoracloud.domain.model.FileCategory
import com.adnan.memoracloud.domain.model.TransferState
import com.adnan.memoracloud.domain.model.TransferType
import com.adnan.memoracloud.domain.model.UserPreferences
import com.adnan.memoracloud.testing.unused
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitReuploadCancelTest {

    private val harness = SyncHarness()
    private val database = harness.database
    private val workDir: File = Files.createTempDirectory("reupload").toFile()
    private val source = File(workDir, "big.vhdx").apply { writeBytes(ByteArray(600) { 7 }) }
    private val deleted = mutableListOf<Long>()

    @After
    fun tearDown() {
        harness.close()
        workDir.deleteRecursively()
    }

    @Test
    fun `cancelling a new upload of a file already in the channel keeps its parts`() = runBlocking {
        record(messageId = 10L, uploadedParts = 2)
        queue()

        executor().execute(database.transferDao().byId("t1")!!)

        assertTrue(deleted.isEmpty())
        assertEquals(2, database.filePartDao().partsOf("f1").size)
    }

    @Test
    fun `cancelling a first upload drops the parts it already sent`() = runBlocking {
        record(messageId = null, uploadedParts = 1)
        queue()

        executor().execute(database.transferDao().byId("t1")!!)

        assertEquals(listOf(10L), deleted)
        assertTrue(database.filePartDao().partsOf("f1").isEmpty())
    }

    private suspend fun record(messageId: Long?, uploadedParts: Int) {
        database.fileDao().upsert(
            FileEntity(
                id = "f1",
                folderId = null,
                name = "big.vhdx",
                sizeBytes = source.length(),
                mimeType = "application/octet-stream",
                category = FileCategory.OTHER,
                localPath = source.absolutePath,
                contentHash = null,
                chatId = 1,
                messageId = messageId,
                remoteFileId = messageId?.let { "r0" },
                remoteUniqueId = messageId?.let { "u0" },
                backupState = if (messageId != null) BackupState.BACKED_UP else BackupState.QUEUED,
                partCount = 2,
                createdAt = 1,
                modifiedAt = 1,
                addedAt = 1
            )
        )
        repeat(uploadedParts) { index ->
            database.filePartDao().upsert(
                FilePartEntity(
                    fileId = "f1",
                    partIndex = index,
                    chatId = 1,
                    messageId = 10L + index,
                    remoteFileId = "r$index",
                    remoteUniqueId = "u$index",
                    plainOffset = index * 300L,
                    plainSize = 300,
                    storedSize = 300,
                    uploadedAt = 1
                )
            )
        }
    }

    private suspend fun queue() {
        database.transferDao().upsert(
            TransferEntity(
                id = "t1",
                type = TransferType.BACKUP,
                fileId = "f1",
                displayName = "big.vhdx",
                localPath = null,
                chatId = 1,
                messageId = null,
                remoteFileId = null,
                sizeBytes = source.length(),
                state = TransferState.CANCELLED,
                createdAt = 1,
                updatedAt = 1
            )
        )
    }

    private fun executor(): TransferExecutor {
        val settings = FakeSettings(UserPreferences(storageChatId = 1))
        val codec = ManifestCodec(StreamCrypto(), unused())
        val telegram = object : TelegramClient by unused() {
            override suspend fun deleteMessages(chatId: Long, messageIds: List<Long>) {
                deleted += messageIds
            }
        }
        val thumbnails = object : ThumbnailStore {
            override suspend fun thumbnailBytes(fileId: String): ByteArray? = null
            override suspend fun uploadThumbnailFile(fileId: String): File? = null
            override suspend fun storeFromDecrypted(fileId: String, plain: File) = Unit
            override suspend fun storeFromStream(
                fileId: String,
                parts: List<MediaPart>,
                encrypted: Boolean
            ) = Unit
        }
        val paths = object : AppStoragePaths by unused() {
            override val filesDir: File = workDir
            override val cacheDir: File = workDir
        }
        val dispatchers = object : DispatcherProvider {
            override val main = Dispatchers.Unconfined
            override val io = Dispatchers.IO
            override val default = Dispatchers.Default
        }
        val icons = ApkIconUploader(unused(), unused(), StreamCrypto(), unused(), unused())
        return TransferExecutor(
            messages = namedErrorMessages(),
            storagePaths = paths,
            telegramClient = telegram,
            transferDao = database.transferDao(),
            fileDao = database.fileDao(),
            backupDao = database.backupDao(),
            manifestCodec = codec,
            folderPathResolver = FolderPathResolver(
                database.folderDao(),
                ActiveChannel(settings),
                database.folderTombstoneDao()
            ),
            thumbnailStore = thumbnails,
            streamCrypto = StreamCrypto(),
            wrappedKeyRepository = unused(),
            downloadWriter = unused(),
            fileImporter = unused(),
            settingsRepository = settings,
            filePartDao = database.filePartDao(),
            partUploader = PartUploader(
                paths, telegram, database.filePartDao(), codec, StreamCrypto(), unused(),
                thumbnails, dispatchers, icons
            ),
            partDownloader = PartDownloader(
                telegram, database.filePartDao(), StreamCrypto(), unused(), dispatchers
            ),
            apkIconUploader = icons,
            localCopyDeleter = unused()
        )
    }
}
