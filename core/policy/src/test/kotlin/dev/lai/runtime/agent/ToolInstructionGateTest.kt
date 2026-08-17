package dev.lai.runtime.agent

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolInstructionGateTest {

    @Test
    fun `plain greetings and questions do not pay the tool-instruction prefill`() {
        assertFalse(ToolInstructionGate.shouldIncludeInstruction("hi"))
        assertFalse(ToolInstructionGate.shouldIncludeInstruction("Hello, how are you?"))
        assertFalse(ToolInstructionGate.shouldIncludeInstruction("হাই"))
        assertFalse(ToolInstructionGate.shouldIncludeInstruction("আসসালামু আলাইকুম"))
        assertFalse(ToolInstructionGate.shouldIncludeInstruction("What is the capital of France?"))
        assertFalse(ToolInstructionGate.shouldIncludeInstruction("বাংলাদেশের রাজধানী কোথায়?"))
        assertFalse(ToolInstructionGate.shouldIncludeInstruction("Tell me a short story"))
        assertFalse(ToolInstructionGate.shouldIncludeInstruction(""))
        assertFalse(ToolInstructionGate.shouldIncludeInstruction("   "))
    }

    @Test
    fun `english word boundaries avoid substring false positives`() {
        // "happened" must not match "app"/"pen"/"open"; "prototype" must not match "type".
        assertFalse(ToolInstructionGate.shouldIncludeInstruction("It happened yesterday"))
        assertFalse(ToolInstructionGate.shouldIncludeInstruction("I like the prototype design"))
        assertFalse(ToolInstructionGate.shouldIncludeInstruction("That was unstoppable"))
    }

    @Test
    fun `english action requests include the instruction`() {
        assertTrue(ToolInstructionGate.shouldIncludeInstruction("open the settings"))
        assertTrue(ToolInstructionGate.shouldIncludeInstruction("Open Settings."))
        assertTrue(ToolInstructionGate.shouldIncludeInstruction("click the login button"))
        assertTrue(ToolInstructionGate.shouldIncludeInstruction("take a screenshot"))
        assertTrue(ToolInstructionGate.shouldIncludeInstruction("scroll down please"))
        assertTrue(ToolInstructionGate.shouldIncludeInstruction("launch the camera app"))
        assertTrue(ToolInstructionGate.shouldIncludeInstruction("go back"))
        assertTrue(ToolInstructionGate.shouldIncludeInstruction("turn off wifi"))
        assertTrue(ToolInstructionGate.shouldIncludeInstruction("type my name in the field"))
        assertTrue(ToolInstructionGate.shouldIncludeInstruction("what is on my screen"))
    }

    @Test
    fun `bangla action requests include the instruction across verb inflections`() {
        assertTrue(ToolInstructionGate.shouldIncludeInstruction("সেটিংস খুলুন"))
        assertTrue(ToolInstructionGate.shouldIncludeInstruction("ক্যামেরা খুলে দাও"))
        assertTrue(ToolInstructionGate.shouldIncludeInstruction("বাটনে ক্লিক করো"))
        assertTrue(ToolInstructionGate.shouldIncludeInstruction("একটা স্ক্রিনশট নাও"))
        assertTrue(ToolInstructionGate.shouldIncludeInstruction("নিচে স্ক্রল করুন"))
        assertTrue(ToolInstructionGate.shouldIncludeInstruction("ওয়াইফাই বন্ধ করো"))
        assertTrue(ToolInstructionGate.shouldIncludeInstruction("স্ক্রিনে কী আছে পড়ে শোনাও"))
        assertTrue(ToolInstructionGate.shouldIncludeInstruction("অ্যাপটা চালু করুন"))
    }
}
