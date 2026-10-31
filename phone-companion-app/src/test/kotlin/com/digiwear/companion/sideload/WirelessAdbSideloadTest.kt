package com.digiwear.companion.sideload

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Comprehensive test suite verifying Wireless ADB Key Management,
 * APK source metadata handling, and Sideload Connection states.
 */
class WirelessAdbSideloadTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mockContext: Context
    private lateinit var fakeFilesDir: File
    private lateinit var fakeCacheDir: File

    @Before
    fun setUp() {
        fakeFilesDir = tempFolder.newFolder("files")
        fakeCacheDir = tempFolder.newFolder("cache")

        // Lightweight mock Context returning our temporary folders
        mockContext = object : android.content.ContextWrapper(null) {
            override fun getFilesDir(): File = fakeFilesDir
            override fun getCacheDir(): File = fakeCacheDir
        }
    }

    @Test
    fun testAdbKeyManagerGeneratesAndPersistsKeys() {
        val keyManager = AdbKeyManager(mockContext)

        val keyPair = keyManager.getOrCreateKeyPair()
        assertNotNull("Generated key pair should not be null", keyPair)

        assertTrue("Private key file should exist", keyManager.privateKeyFile.exists())
        assertTrue("Public key file should exist", keyManager.publicKeyFile.exists())
        assertTrue("Private key file should not be empty", keyManager.privateKeyFile.length() > 0)
        assertTrue("Public key file should not be empty", keyManager.publicKeyFile.length() > 0)

        // Verify SHA-256 fingerprint formatting (32 hex bytes separated by colons: 32*2 + 31 = 95 chars)
        val sha256 = keyManager.getPublicKeyFingerprintSha256()
        assertNotNull(sha256)
        assertTrue("SHA-256 fingerprint should contain colons", sha256.contains(":"))
        assertEquals("SHA-256 fingerprint should have 32 colon-delimited hex octets", 32, sha256.split(":").size)

        // Verify MD5 fingerprint formatting (16 hex bytes separated by colons: 16*2 + 15 = 47 chars)
        val md5 = keyManager.getPublicKeyFingerprintMd5()
        assertNotNull(md5)
        assertTrue("MD5 fingerprint should contain colons", md5.contains(":"))
        assertEquals("MD5 fingerprint should have 16 colon-delimited hex octets", 16, md5.split(":").size)

        // Re-reading from disk returns the same key
        val keyPair2 = keyManager.getOrCreateKeyPair()
        assertNotNull(keyPair2)
        assertEquals("Fingerprint must remain consistent across reads", sha256, keyManager.getPublicKeyFingerprintSha256())
    }

    @Test
    fun testWatchApkInfoFormatting() {
        val dummyFile = tempFolder.newFile("dummy.apk")

        val zeroInfo = WatchApkInfo(dummyFile, "zero.apk", 0L, ApkSourceType.PHONE_STORAGE)
        assertEquals("0 B", zeroInfo.sizeFormatted)

        val oneMbInfo = WatchApkInfo(dummyFile, "app.apk", 1024L * 1024L, ApkSourceType.GITHUB_DOWNLOAD)
        assertEquals("1.00 MB", oneMbInfo.sizeFormatted)

        val watchApkSize = 60071973L // 60.07 MB actual wear-os-app-release.apk
        val releaseInfo = WatchApkInfo(dummyFile, "DigiWear-WearOS-release.apk", watchApkSize, ApkSourceType.EMBEDDED_ASSET)
        assertEquals("57.29 MB", releaseInfo.sizeFormatted)
    }

    @Test
    fun testApkSourceTypeLabels() {
        assertEquals("Phone Storage", ApkSourceType.PHONE_STORAGE.label)
        assertEquals("GitHub release_bin", ApkSourceType.GITHUB_DOWNLOAD.label)
        assertEquals("Embedded Asset", ApkSourceType.EMBEDDED_ASSET.label)
        assertEquals("Local Cache", ApkSourceType.LOCAL_CACHE.label)
    }

    @Test
    fun testWirelessAdbManagerInitialStateAndLogs() {
        val adbManager = WirelessAdbManager(mockContext)

        assertEquals("Initial state must be IDLE", SideloadConnectionState.IDLE, adbManager.connectionState.value)
        assertFalse("Must not be connected initially", adbManager.isConnected())
        assertTrue("Initial logs should be empty", adbManager.consoleLogs.value.isEmpty())

        adbManager.log("Test log entry 1")
        adbManager.log("Test log entry 2")

        assertEquals(2, adbManager.consoleLogs.value.size)
        assertTrue("Log entry must contain message text", adbManager.consoleLogs.value[0].contains("Test log entry 1"))
        assertTrue("Log entry must contain timestamp bracket", adbManager.consoleLogs.value[0].startsWith("["))

        adbManager.clearLogs()
        assertTrue("Logs should be empty after clear", adbManager.consoleLogs.value.isEmpty())
    }

    @Test
    fun testConnectedWatchDetailsDefaults() {
        val details = ConnectedWatchDetails(
            model = "Pixel Watch 2",
            manufacturer = "Google",
            androidVersion = "14",
            sdkVersion = 34,
            wearPlatform = "Wear OS 5.0",
            isDigiWearInstalled = true,
            installedVersionName = "v0.9.0",
            batteryPercent = "92%"
        )

        assertEquals("Pixel Watch 2", details.model)
        assertEquals("Google", details.manufacturer)
        assertEquals("14", details.androidVersion)
        assertEquals(34, details.sdkVersion)
        assertEquals("Wear OS 5.0", details.wearPlatform)
        assertTrue(details.isDigiWearInstalled)
        assertEquals("v0.9.0", details.installedVersionName)
        assertEquals("92%", details.batteryPercent)
    }

    @Test
    fun testSideloadConnectionStateEnumLabels() {
        assertEquals("Disconnected", SideloadConnectionState.IDLE.label)
        assertEquals("Connecting to Watch...", SideloadConnectionState.CONNECTING.label)
        assertEquals("Connected to Watch", SideloadConnectionState.CONNECTED.label)
        assertEquals("Streaming APK to Watch...", SideloadConnectionState.TRANSFERRING.label)
        assertEquals("Installing Package (-r -d -g)...", SideloadConnectionState.INSTALLING.label)
        assertEquals("Granting Sensor & Notification Permissions...", SideloadConnectionState.GRANTING_PERMISSIONS.label)
        assertEquals("Installation Successful!", SideloadConnectionState.SUCCESS.label)
        assertEquals("Error", SideloadConnectionState.ERROR.label)
    }

    @Test
    fun testAdbKeyManagerPrivateKeyAndCertificate() {
        val keyManager = AdbKeyManager(mockContext)

        val privateKey = keyManager.getPrivateKey()
        assertNotNull("Private key must not be null", privateKey)
        assertEquals("RSA", privateKey.algorithm)

        val cert = keyManager.getCertificate()
        assertNotNull("Certificate must not be null", cert)
        assertTrue("Certificate file adbkey.crt must exist", keyManager.certificateFile.exists())
        assertTrue("Certificate file adbkey.crt must not be empty", keyManager.certificateFile.length() > 0)

        // Subject DN and Issuer DN check
        assertTrue("Certificate subject must contain DigiWear-Companion", cert.subjectDN.name.contains("DigiWear-Companion"))
        assertTrue("Certificate issuer must contain DigiWear-Companion", cert.issuerDN.name.contains("DigiWear-Companion"))

        // Validity check (approx 10 years ~ 3650 days)
        val validityDays = (cert.notAfter.time - cert.notBefore.time) / (1000L * 60 * 60 * 24)
        assertTrue("Certificate validity must be approximately 10 years (got $validityDays days)", validityDays in 3640..3660)

        // Verify self-signed
        val publicKey = keyManager.getPublicKey()
        cert.verify(publicKey)

        // Persistence check - re-reading gives identical certificate
        val cert2 = keyManager.getCertificate()
        assertEquals("Certificate must match persisted file", cert, cert2)
    }

    @Test
    fun testDigiWearAdbConnectionManagerConfiguration() {
        val keyManager = AdbKeyManager(mockContext)
        val connectionManager = DigiWearAdbConnectionManager(keyManager)

        assertEquals("DigiWear-Companion", connectionManager.getDeviceName())
        connectionManager.setDeviceName("Custom-Wear-Client")
        assertEquals("Custom-Wear-Client", connectionManager.getDeviceName())

        assertEquals(180000L, connectionManager.timeout)
        assertTrue("API level should be >= 26", connectionManager.api >= 26)

        assertNotNull(connectionManager.getPrivateKey())
        assertNotNull(connectionManager.getCertificate())
        assertEquals(keyManager.getPrivateKey(), connectionManager.getPrivateKey())
    }

    @Test
    fun testWirelessAdbManagerPairInputValidation() = kotlinx.coroutines.runBlocking {
        val adbManager = WirelessAdbManager(mockContext)

        // Empty IP
        val emptyIpResult = adbManager.pair("", 38912, "123456")
        assertFalse(emptyIpResult)
        assertNotNull(adbManager.lastError.value)
        assertTrue(adbManager.lastError.value!!.contains("IP address is required", ignoreCase = true))

        // Non-6-digit code
        val shortCodeResult = adbManager.pair("192.168.1.100", 38912, "1234")
        assertFalse(shortCodeResult)
        assertTrue(adbManager.lastError.value!!.contains("6 digits", ignoreCase = true))

        val nonNumericCodeResult = adbManager.pair("192.168.1.100", 38912, "abcdef")
        assertFalse(nonNumericCodeResult)
        assertTrue(adbManager.lastError.value!!.contains("6 digits", ignoreCase = true))

        // Invalid port
        val invalidPortResult = adbManager.pair("192.168.1.100", 0, "123456")
        assertFalse(invalidPortResult)
        assertTrue(adbManager.lastError.value!!.contains("port", ignoreCase = true))

        val overflowPortResult = adbManager.pair("192.168.1.100", 70000, "123456")
        assertFalse(overflowPortResult)
        assertTrue(adbManager.lastError.value!!.contains("port", ignoreCase = true))
    }

    @Test
    fun testInstallApkDisconnectedFailsGracefully() = kotlinx.coroutines.runBlocking {
        val adbManager = WirelessAdbManager(mockContext)
        val dummyApk = File(tempFolder.root, "dummy.apk").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }

        // Test installApk alias
        val installResult = adbManager.installApk(dummyApk)
        assertFalse("installApk must fail when not connected", installResult)
        assertEquals("Watch is not connected", adbManager.lastError.value)

        // Test sideloadApk method
        val sideloadResult = adbManager.sideloadApk(dummyApk)
        assertFalse("sideloadApk must fail when not connected", sideloadResult)
        assertEquals("Watch is not connected", adbManager.lastError.value)
    }

    @Test
    fun testDigiWearAdbConnectionManagerInstallApkNonexistentFile() {
        val keyManager = AdbKeyManager(mockContext)
        val connectionManager = DigiWearAdbConnectionManager(keyManager)
        val nonExistent = File(tempFolder.root, "does_not_exist.apk")

        val result = connectionManager.installApk(nonExistent)
        assertTrue("Result should be failure for nonexistent file", result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("does not exist") == true)
    }

    @Test
    fun testAdbKeyManagerPemPrivateKeyParsing() {
        val keyManager = AdbKeyManager(mockContext)
        val origPrivKey = keyManager.getPrivateKey()
        assertNotNull(origPrivKey)

        // Convert to PEM format and write back to privateKeyFile
        val b64 = java.util.Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(origPrivKey.encoded)
        val pem = "-----BEGIN PRIVATE KEY-----\n$b64\n-----END PRIVATE KEY-----\n"
        keyManager.privateKeyFile.writeText(pem)

        // Re-read private key from PEM
        val parsedPrivKey = keyManager.getPrivateKey()
        assertNotNull(parsedPrivKey)
        assertEquals("RSA", parsedPrivKey.algorithm)
        assertEquals(origPrivKey, parsedPrivKey)

        // Derived public key must still match
        val pubKey = keyManager.getPublicKey()
        assertNotNull(pubKey)
        assertEquals("RSA", pubKey.algorithm)
    }

    @Test
    fun testIpAndPortStringParsing() {
        val input = "192.168.0.42:38841"
        assertTrue(input.contains(":"))
        val parts = input.split(":")
        val ip = parts[0].trim()
        val port = parts[1].filter { it.isDigit() }
        assertEquals("192.168.0.42", ip)
        assertEquals("38841", port)
    }
}
