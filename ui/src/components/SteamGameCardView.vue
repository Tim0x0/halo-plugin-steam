<script setup lang="ts">
import { NodeViewWrapper } from "@halo-dev/richtext-editor";
import { ref, watch, onMounted, computed } from "vue";

const props = defineProps<{
  editor: any;
  node: any;
  updateAttributes: (attrs: Record<string, any>) => void;
  selected: boolean;
  getPos: () => number;
  deleteNode: () => void;
}>();

interface GameDetail {
  appId: number;
  name: string;
  headerImage: string;
  shortDescription: string;
  developers: string;
  publishers: string;
  genres: string;
  isFree: boolean;
  priceFormatted: string;
  releaseDate: string;
  storeUrl: string;
  owned: boolean;
  playtimeForever: number;
  playtimeFormatted: string;
  rtimeLastPlayed: number;
  lastPlayedFormatted: string;
  achievedCount: number;
  totalAchievements: number;
  achievementProgress: string;
}

const data = ref<GameDetail | null>(null);
const loading = ref(false);
const error = ref("");
const editing = ref(false);
const inputAppId = ref("");

const appId = computed(() => props.node.attrs.appId);
const theme = computed(() => props.node.attrs.theme || "steam-dark");

function detectSteamLanguage(): string {
  const lang = (navigator.language || "en").toLowerCase();
  if (lang === "zh-cn" || lang === "zh" || lang.startsWith("zh-hans")) return "schinese";
  if (lang === "zh-tw" || lang === "zh-hk" || lang.startsWith("zh-hant")) return "tchinese";
  if (lang.startsWith("ja")) return "japanese";
  if (lang.startsWith("ko")) return "koreana";
  if (lang.startsWith("de")) return "german";
  if (lang.startsWith("fr")) return "french";
  return "english";
}

async function fetchData(id: string | number) {
  if (!id) return;
  loading.value = true;
  error.value = "";
  try {
    const lang = detectSteamLanguage();
    const resp = await fetch(`/apis/api.steam.timxs.com/v1alpha1/game-detail/${id}?lang=${lang}`);
    if (!resp.ok) throw new Error(`请求失败: ${resp.status}`);
    data.value = await resp.json();
  } catch (e: any) {
    error.value = e.message || "加载失败";
    data.value = null;
  } finally {
    loading.value = false;
  }
}

function confirmAppId() {
  const val = inputAppId.value.trim();
  if (!val) return;
  // 支持从 Steam URL 提取 appId
  let id = val;
  const match = val.match(/\/app\/(\d+)/);
  if (match) {
    id = match[1];
  }
  if (!/^\d+$/.test(id)) {
    error.value = "请输入有效的 App ID 或 Steam 商店链接";
    return;
  }
  props.updateAttributes({ appId: id });
  editing.value = false;
  fetchData(id);
}

function openSettings() {
  editing.value = true;
  inputAppId.value = appId.value || "";
}

watch(appId, (newVal) => {
  if (newVal) fetchData(newVal);
}, { immediate: false });

onMounted(() => {
  if (appId.value) {
    fetchData(appId.value);
  } else {
    editing.value = true;
  }
});
</script>

<template>
  <NodeViewWrapper as="div" class="steam-game-card-wrapper" :class="{ selected }">
    <!-- 未配置 appId 或编辑模式 -->
    <div v-if="!appId || editing" class="steam-card-config">
      <div class="config-header">
        <svg viewBox="0 0 496.66 496.66" width="20" height="20">
          <path fill="#66c0f4" d="M247.92 0C114.08 0 5.07 104.57.06 236.68l133.05 55.07a68.25 68.25 0 0 1 38.59-11.87l.24-.01 57.89-83.88v-1.18c0-56.98 46.34-103.32 103.32-103.32s103.32 46.34 103.32 103.32c0 56.98-46.34 103.32-103.32 103.32h-2.4l-82.54 58.87.01.16c0 37.62-30.09 68.27-67.71 68.93l-.57.01c-27.1 0-50.68-16.03-61.42-39.1L4.21 340.55C31.85 432.2 116.93 496.66 216.72 496.66c137 0 248.18-111.18 248.18-248.18S384.92 0 247.92 0z"/>
        </svg>
        <span>Steam 游戏卡片</span>
      </div>
      <div class="config-body">
        <div class="input-row">
          <input
            v-model="inputAppId"
            type="text"
            placeholder="输入 App ID 或 Steam 商店链接"
            @keyup.enter="confirmAppId"
          />
          <button class="btn-primary" @click="confirmAppId">确认</button>
          <button v-if="appId" class="btn-secondary" @click="editing = false">取消</button>
        </div>
        <div class="config-options">
          <label>
            <span>主题:</span>
            <select :value="theme" @change="updateAttributes({ theme: ($event.target as HTMLSelectElement).value })">
              <option value="steam-dark">Steam 深色</option>
              <option value="adaptive">自适应</option>
            </select>
          </label>
          <div class="toggle-group">
            <label v-for="opt in [
              { key: 'showDescription', label: '描述' },
              { key: 'showPlaytime', label: '时长' },
              { key: 'showAchievements', label: '成就' },
              { key: 'showLastPlayed', label: '最后游玩' },
              { key: 'showPrice', label: '价格' },
              { key: 'showDeveloper', label: '开发商' },
            ]" :key="opt.key" class="toggle-item">
              <input
                type="checkbox"
                :checked="node.attrs[opt.key] !== false"
                @change="updateAttributes({ [opt.key]: ($event.target as HTMLInputElement).checked })"
              />
              <span>{{ opt.label }}</span>
            </label>
          </div>
        </div>
        <p v-if="error" class="error-msg">{{ error }}</p>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-else-if="loading" class="steam-card-loading">
      <div class="skeleton-image"></div>
      <div class="skeleton-content">
        <div class="skeleton-line w-60"></div>
        <div class="skeleton-line w-100"></div>
        <div class="skeleton-line w-40"></div>
      </div>
    </div>

    <!-- 加载失败 -->
    <div v-else-if="error && !data" class="steam-card-error">
      <p>{{ error }}</p>
      <button @click="fetchData(appId)">重试</button>
    </div>

    <!-- 预览卡片 -->
    <div v-else-if="data" class="steam-card-preview" :class="{ 'theme-dark': theme === 'steam-dark' }">
      <img v-if="data.headerImage" :src="data.headerImage" :alt="data.name" class="card-image" />
      <div class="card-info">
        <h4 class="card-title">{{ data.name }}</h4>
        <p v-if="node.attrs.showDescription && data.shortDescription" class="card-desc">{{ data.shortDescription }}</p>
        <div v-if="data.genres" class="card-genres">
          <span v-for="g in data.genres.split(', ').slice(0, 4)" :key="g" class="genre-tag">{{ g }}</span>
        </div>
        <div class="card-meta">
          <span v-if="node.attrs.showPrice && data.priceFormatted">{{ data.priceFormatted }}</span>
          <span v-if="node.attrs.showDeveloper && data.developers">{{ data.developers }}</span>
          <span v-if="data.releaseDate">{{ data.releaseDate }}</span>
        </div>
        <div v-if="data.owned" class="card-personal">
          <span v-if="node.attrs.showPlaytime && data.playtimeFormatted">{{ data.playtimeFormatted }}</span>
          <span v-if="node.attrs.showAchievements && data.achievementProgress">{{ data.achievementProgress }}</span>
          <span v-if="node.attrs.showLastPlayed && data.lastPlayedFormatted">{{ data.lastPlayedFormatted }}</span>
        </div>
      </div>
      <button class="edit-btn" @click="openSettings" title="编辑">&#9998;</button>
    </div>
  </NodeViewWrapper>
</template>

<style scoped>
.steam-game-card-wrapper {
  margin: 12px 0;
  position: relative;
}
.steam-game-card-wrapper.selected {
  outline: 2px solid #66c0f4;
  border-radius: 8px;
}

/* 配置面板 */
.steam-card-config {
  border: 2px dashed #d1d5db;
  border-radius: 8px;
  overflow: hidden;
}
.config-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #1b2838;
  color: #c7d5e0;
  font-weight: 600;
}
.config-body {
  padding: 16px;
  background: #f9fafb;
}
.input-row {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.input-row input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
}
.btn-primary {
  padding: 8px 16px;
  background: #1a9fff;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}
.btn-primary:hover { background: #0d8ce0; }
.btn-secondary {
  padding: 8px 16px;
  background: #e5e7eb;
  color: #374151;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}
.config-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.config-options label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}
.config-options select {
  padding: 4px 8px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
}
.toggle-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.toggle-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #4b5563;
}
.error-msg {
  color: #ef4444;
  margin-top: 8px;
  font-size: 13px;
}

/* 加载状态 */
.steam-card-loading {
  display: flex;
  gap: 16px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f9fafb;
}
.skeleton-image {
  width: 200px;
  height: 94px;
  background: linear-gradient(90deg, #e5e7eb 25%, #f3f4f6 50%, #e5e7eb 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 4px;
  flex-shrink: 0;
}
.skeleton-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.skeleton-line {
  height: 14px;
  background: linear-gradient(90deg, #e5e7eb 25%, #f3f4f6 50%, #e5e7eb 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 4px;
}
.w-60 { width: 60%; }
.w-100 { width: 100%; }
.w-40 { width: 40%; }
@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

/* 错误状态 */
.steam-card-error {
  padding: 24px;
  text-align: center;
  border: 1px solid #fecaca;
  border-radius: 8px;
  background: #fef2f2;
  color: #b91c1c;
}
.steam-card-error button {
  margin-top: 8px;
  padding: 6px 16px;
  background: #ef4444;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

/* 预览卡片 */
.steam-card-preview {
  display: flex;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  background: white;
  position: relative;
}
.steam-card-preview.theme-dark {
  background: #1b2838;
  border-color: #2a475e;
  color: #c7d5e0;
}
.card-image {
  width: 230px;
  object-fit: cover;
  flex-shrink: 0;
}
.card-info {
  flex: 1;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}
.card-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.theme-dark .card-title { color: #ffffff; }
.card-desc {
  margin: 0;
  font-size: 12px;
  color: #6b7280;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.theme-dark .card-desc { color: #8f98a0; }
.card-genres {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.genre-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 3px;
  background: #e5e7eb;
  color: #4b5563;
}
.theme-dark .genre-tag {
  background: #2a475e;
  color: #66c0f4;
}
.card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 12px;
  color: #9ca3af;
}
.theme-dark .card-meta { color: #8f98a0; }
.card-personal {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 12px;
  padding-top: 6px;
  border-top: 1px solid #e5e7eb;
  color: #4b5563;
}
.theme-dark .card-personal {
  border-top-color: #2a475e;
  color: #acdbf5;
}
.edit-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  background: rgba(0,0,0,0.5);
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  opacity: 0;
  transition: opacity 0.2s;
}
.steam-card-preview:hover .edit-btn { opacity: 1; }
</style>
