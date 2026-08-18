package dev.lai.runtime.workspace

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AtomicNamedDocumentReplaceTest {

    private val replace = AtomicNamedDocumentReplace()
    private val target = WorkspaceLayout.SETTINGS_FILE_NAME
    private val temp = WorkspaceLayout.SETTINGS_TEMP_FILE_NAME
    private val backup = WorkspaceLayout.SETTINGS_BACKUP_FILE_NAME
    private val good = "good-settings".toByteArray()
    private val next = "next-settings".toByteArray()

    @Test
    fun `first save creates only the target`() {
        val store = MemoryStore()
        replace.replace(store, target, temp, backup, good)
        assertArrayEquals(good, store.bytes(target))
        assertNull(store.find(temp))
        assertNull(store.find(backup))
    }

    @Test
    fun `successful replace keeps new bytes and drops backup`() {
        val store = MemoryStore()
        store.put(target, good)
        replace.replace(store, target, temp, backup, next)
        assertArrayEquals(next, store.bytes(target))
        assertNull(store.find(backup))
        assertNull(store.find(temp))
    }

    @Test
    fun `failed finalize restores last known good from backup`() {
        val store = MemoryStore(failRenameFromTo = setOf(temp to target))
        store.put(target, good)
        try {
            replace.replace(store, target, temp, backup, next)
            fail("expected finalize failure")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message!!.contains("finalize"))
        }
        assertArrayEquals(good, store.bytes(target))
        assertNull(store.find(temp))
    }

    @Test
    fun `failed park of existing target leaves last known good in place`() {
        val store = MemoryStore(failRenameFromTo = setOf(target to backup))
        store.put(target, good)
        try {
            replace.replace(store, target, temp, backup, next)
            fail("expected park failure")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message!!.contains("park"))
        }
        assertArrayEquals(good, store.bytes(target))
        assertNull(store.find(temp))
    }

    @Test
    fun `resolveReadable restores target from leftover backup`() {
        val store = MemoryStore()
        store.put(backup, good)
        val handle = replace.resolveReadable(store, target, backup)
        assertNotNull(handle)
        assertArrayEquals(good, store.bytes(target))
        assertNull(store.find(backup))
    }

    @Test
    fun `resolveReadable prefers existing target over backup`() {
        val store = MemoryStore()
        store.put(target, next)
        store.put(backup, good)
        val handle = replace.resolveReadable(store, target, backup)
        assertEquals(store.find(target), handle)
        assertArrayEquals(next, store.bytes(target))
    }

    @Test
    fun `stale temp from a previous attempt is overwritten`() {
        val store = MemoryStore()
        store.put(temp, "stale".toByteArray())
        store.put(target, good)
        replace.replace(store, target, temp, backup, next)
        assertArrayEquals(next, store.bytes(target))
        assertNull(store.find(temp))
    }

    private class MemoryStore(
        private val failRenameFromTo: Set<Pair<String, String>> = emptySet(),
    ) : AtomicNamedDocumentReplace.Store {
        private val names = LinkedHashMap<String, ByteArray>()
        private val idToName = HashMap<String, String>()
        private var nextId = 1

        fun put(name: String, bytes: ByteArray) {
            val id = "id-${nextId++}"
            names[name] = bytes.copyOf()
            idToName[id] = name
        }

        fun bytes(name: String): ByteArray? = names[name]

        override fun find(name: String): AtomicNamedDocumentReplace.Handle? {
            if (name !in names) return null
            val id = idToName.entries.first { it.value == name }.key
            return AtomicNamedDocumentReplace.Handle(id)
        }

        override fun create(name: String): AtomicNamedDocumentReplace.Handle {
            require(name !in names) { "already exists: $name" }
            val id = "id-${nextId++}"
            names[name] = ByteArray(0)
            idToName[id] = name
            return AtomicNamedDocumentReplace.Handle(id)
        }

        override fun write(handle: AtomicNamedDocumentReplace.Handle, bytes: ByteArray) {
            val name = idToName[handle.id] ?: error("unknown handle")
            names[name] = bytes.copyOf()
        }

        override fun delete(handle: AtomicNamedDocumentReplace.Handle): Boolean {
            val name = idToName.remove(handle.id) ?: return false
            names.remove(name)
            return true
        }

        override fun rename(handle: AtomicNamedDocumentReplace.Handle, newName: String): AtomicNamedDocumentReplace.Handle? {
            val currentName = idToName[handle.id] ?: return null
            if ((currentName to newName) in failRenameFromTo) return null
            if (newName in names) return null
            val bytes = names.remove(currentName) ?: return null
            names[newName] = bytes
            idToName[handle.id] = newName
            return handle
        }
    }
}
