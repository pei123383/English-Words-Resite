<script setup lang="ts">
import { Headset } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'

const props = defineProps<{
  text: string | null | undefined
  size?: 'small' | 'default' | 'large'
}>()

const supported = ref(false)
const voices = ref<SpeechSynthesisVoice[]>([])

const normalizedText = computed(() => props.text?.trim() || '')
const disabled = computed(() => !supported.value || !normalizedText.value)

function loadVoices() {
  if (!supported.value) return
  voices.value = window.speechSynthesis.getVoices()
}

function pickEnglishVoice() {
  const englishVoices = voices.value.filter((voice) => voice.lang.toLowerCase().startsWith('en'))
  return (
    englishVoices.find((voice) => voice.lang.toLowerCase() === 'en-us') ||
    englishVoices.find((voice) => voice.lang.toLowerCase().startsWith('en-us')) ||
    englishVoices[0] ||
    null
  )
}

function speak() {
  if (!normalizedText.value) return
  if (!supported.value) {
    ElMessage.warning('当前浏览器不支持单词读音')
    return
  }

  window.speechSynthesis.cancel()
  const utterance = new SpeechSynthesisUtterance(normalizedText.value)
  utterance.lang = 'en-US'
  utterance.rate = 0.9
  utterance.pitch = 1
  const voice = pickEnglishVoice()
  if (voice) utterance.voice = voice
  window.speechSynthesis.speak(utterance)
}

onMounted(() => {
  supported.value = typeof window !== 'undefined' && 'speechSynthesis' in window && 'SpeechSynthesisUtterance' in window
  loadVoices()
  if (supported.value) {
    window.speechSynthesis.onvoiceschanged = loadVoices
  }
})
</script>

<template>
  <el-tooltip :content="disabled ? '当前浏览器不支持单词读音' : '播放读音'" placement="top">
    <span class="pronunciation-wrap">
      <el-button
        class="pronunciation-button"
        :icon="Headset"
        :size="size || 'small'"
        :disabled="disabled"
        circle
        @click.stop="speak"
      />
    </span>
  </el-tooltip>
</template>

<style scoped>
.pronunciation-wrap {
  display: inline-flex;
  vertical-align: middle;
}

.pronunciation-button {
  flex: 0 0 auto;
}
</style>
