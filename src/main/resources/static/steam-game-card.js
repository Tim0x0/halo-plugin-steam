(function () {
  'use strict';

  class SteamGameCard extends HTMLElement {
    static get observedAttributes() {
      return [
        'app-id', 'theme', 'show-description', 'show-playtime',
        'show-achievements', 'show-last-played', 'show-price', 'show-developer'
      ];
    }

    constructor() {
      super();
      this._shadow = this.attachShadow({ mode: 'open' });
      this._data = null;
      this._observer = null;
      this._isDark = false;
      this._lang = 'english';
    }

    connectedCallback() {
      this.fetchAndRender();
      if (this.getTheme() === 'adaptive') {
        this.setupThemeObserver();
      }
    }

    disconnectedCallback() {
      if (this._observer) {
        this._observer.disconnect();
        this._observer = null;
      }
    }

    attributeChangedCallback(name, oldVal, newVal) {
      if (oldVal !== newVal && this.isConnected) {
        if (name === 'app-id') {
          this.fetchAndRender();
        } else if (name === 'theme') {
          if (newVal === 'adaptive') {
            this.setupThemeObserver();
          } else if (this._observer) {
            this._observer.disconnect();
            this._observer = null;
          }
          this.render();
        } else {
          this.render();
        }
      }
    }

    getTheme() {
      return this.getAttribute('theme') || 'steam-dark';
    }

    getBool(attr) {
      return this.getAttribute(attr) !== 'false';
    }

    setupThemeObserver() {
      this.detectDarkMode();
      if (this._observer) {
        this._observer.disconnect();
      }
      this._observer = new MutationObserver(() => {
        const wasDark = this._isDark;
        this.detectDarkMode();
        if (wasDark !== this._isDark) {
          this.render();
        }
      });
      this._observer.observe(document.documentElement, {
        attributes: true,
        attributeFilter: ['class', 'data-theme', 'data-color-mode', 'data-dark']
      });
    }

    detectDarkMode() {
      var selector = window.__STEAM_DARK_SELECTOR__ || 'html.dark';
      try {
        this._isDark = !!document.querySelector(selector);
      } catch (e) {
        this._isDark = false;
      }
    }

    static detectSteamLanguage() {
      var lang = (navigator.language || 'en').toLowerCase();
      if (lang === 'zh-cn' || lang === 'zh' || lang.startsWith('zh-hans')) return 'schinese';
      if (lang === 'zh-tw' || lang === 'zh-hk' || lang.startsWith('zh-hant')) return 'tchinese';
      if (lang.startsWith('ja')) return 'japanese';
      if (lang.startsWith('ko')) return 'koreana';
      if (lang.startsWith('de')) return 'german';
      if (lang.startsWith('fr')) return 'french';
      return 'english';
    }

    static isChinese(lang) {
      return lang === 'schinese' || lang === 'tchinese';
    }

    static t(key, lang) {
      var zh = lang === 'tchinese';
      var labels = {
        played: zh ? '\u904A\u73A9' : '\u6E38\u73A9',
        hours: zh ? '\u5C0F\u6642' : '\u5C0F\u65F6',
        achievement: zh ? '\u6210\u5C31' : '\u6210\u5C31',
        recent: zh ? '\u6700\u8FD1' : '\u6700\u8FD1',
        loadFailed: zh ? '\u52A0\u8F09\u5931\u6557' : '\u52A0\u8F7D\u5931\u8D25',
        retry: zh ? '\u91CD\u8A66' : '\u91CD\u8BD5',
      };
      var en = {
        played: 'Played',
        hours: 'hrs',
        achievement: 'Achievements',
        recent: 'Last played',
        loadFailed: 'Failed to load',
        retry: 'Retry',
      };
      return SteamGameCard.isChinese(lang) ? labels[key] : en[key];
    }

    async fetchAndRender() {
      var appId = this.getAttribute('app-id') || this.getAttribute('appid');
      if (!appId) return;
      this.renderLoading();
      try {
        var lang = SteamGameCard.detectSteamLanguage();
        this._lang = lang;
        var resp = await fetch('/apis/api.steam.timxs.com/v1alpha1/game-detail/' + appId + '?lang=' + lang);
        if (!resp.ok) throw new Error('HTTP ' + resp.status);
        this._data = await resp.json();
        this.render();
      } catch (e) {
        this.renderError(e.message);
      }
    }

    renderLoading() {
      var dark = this.getTheme() === 'steam-dark' || (this.getTheme() === 'adaptive' && this._isDark);
      this._shadow.innerHTML = '<style>' + this.getStyles() + '</style>' +
        '<div class="card ' + (dark ? 'dark' : 'light') + '">' +
          '<div class="skeleton-wrap">' +
            '<div class="skeleton skeleton-img"></div>' +
            '<div class="skeleton-content">' +
              '<div class="skeleton skeleton-title"></div>' +
              '<div class="skeleton skeleton-text"></div>' +
              '<div class="skeleton skeleton-text short"></div>' +
              '<div class="skeleton skeleton-tags"></div>' +
            '</div>' +
          '</div>' +
        '</div>';
    }

    renderError(msg) {
      var dark = this.getTheme() === 'steam-dark' || (this.getTheme() === 'adaptive' && this._isDark);
      var self = this;
      this._shadow.innerHTML = '<style>' + this.getStyles() + '</style>' +
        '<div class="card ' + (dark ? 'dark' : 'light') + '">' +
          '<div class="error-wrap">' +
            '<div class="error-icon">' +
              '<svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">' +
                '<circle cx="12" cy="12" r="10"/>' +
                '<line x1="12" y1="8" x2="12" y2="12"/>' +
                '<line x1="12" y1="16" x2="12.01" y2="16"/>' +
              '</svg>' +
            '</div>' +
            '<div class="error-msg">' + SteamGameCard.t('loadFailed', this._lang) + '</div>' +
            '<div class="error-detail">' + this.escapeHtml(msg) + '</div>' +
            '<button class="retry-btn">' + SteamGameCard.t('retry', this._lang) + '</button>' +
          '</div>' +
        '</div>';
      var btn = this._shadow.querySelector('.retry-btn');
      if (btn) {
        btn.addEventListener('click', function () {
          self.fetchAndRender();
        });
      }
    }

    render() {
      var d = this._data;
      if (!d) return;
      var lang = this._lang;

      var dark = this.getTheme() === 'steam-dark' || (this.getTheme() === 'adaptive' && this._isDark);
      var showDesc = this.getBool('show-description');
      var showPlaytime = this.getBool('show-playtime');
      var showAchievements = this.getBool('show-achievements');
      var showLastPlayed = this.getBool('show-last-played');
      var showPrice = this.getBool('show-price');
      var showDeveloper = this.getBool('show-developer');

      var storeUrl = 'https://store.steampowered.com/app/' + (d.appId || '');

      // 构建标签 HTML
      var genresHtml = '';
      if (d.genres && d.genres.length > 0) {
        var genreList = d.genres.split(', ');
        genresHtml = '<div class="genres">';
        for (var i = 0; i < genreList.length && i < 4; i++) {
          genresHtml += '<span class="genre-tag">' + this.escapeHtml(genreList[i]) + '</span>';
        }
        genresHtml += '</div>';
      }

      // 构建信息区域
      var metaItems = '';
      if (showPrice) {
        var priceText = d.priceFormatted || '';
        if (priceText) {
          metaItems += '<span class="meta-item">' +
            '<svg class="meta-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>' +
            this.escapeHtml(priceText) + '</span>';
        }
      }
      if (showDeveloper && d.developers) {
        metaItems += '<span class="meta-item">' +
          '<svg class="meta-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 18l6-6-6-6"/><path d="M8 6l-6 6 6 6"/></svg>' +
          this.escapeHtml(d.developers) + '</span>';
      }
      if (d.releaseDate) {
        metaItems += '<span class="meta-item">' +
          '<svg class="meta-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>' +
          this.escapeHtml(d.releaseDate) + '</span>';
      }

      // 构建个人数据区域
      var personalHtml = '';
      if (d.owned) {
        var personalItems = '';
        var T = function(k) { return SteamGameCard.t(k, lang); };
        if (showPlaytime && d.playtimeForever != null) {
          var hours = Math.round(d.playtimeForever / 60 * 10) / 10;
          personalItems += '<span class="personal-item">' +
            '<svg class="meta-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>' +
            T('played') + ' ' + hours + ' ' + T('hours') + '</span>';
        }
        if (showAchievements && d.achievementProgress != null) {
          personalItems += '<span class="personal-item">' +
            '<svg class="meta-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 9H4.5a2.5 2.5 0 0 1 0-5C7 4 7 7 7 7"/><path d="M18 9h1.5a2.5 2.5 0 0 0 0-5C17 4 17 7 17 7"/><path d="M4 22h16"/><path d="M10 14.66V17c0 .55-.47.98-.97 1.21C7.85 18.75 7 20 7 22"/><path d="M14 14.66V17c0 .55.47.98.97 1.21C16.15 18.75 17 20 17 22"/><path d="M18 2H6v7a6 6 0 0 0 12 0V2Z"/></svg>' +
            T('achievement') + ' ' + this.escapeHtml(d.achievementProgress) + '</span>';
        }
        if (showLastPlayed && d.lastPlayedFormatted) {
          personalItems += '<span class="personal-item">' +
            '<svg class="meta-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 12h14"/><path d="M12 5l7 7-7 7"/></svg>' +
            T('recent') + ' ' + this.escapeHtml(d.lastPlayedFormatted) + '</span>';
        }
        if (personalItems) {
          personalHtml = '<div class="personal-section">' + personalItems + '</div>';
        }
      }

      this._shadow.innerHTML = '<style>' + this.getStyles() + '</style>' +
        '<a class="card ' + (dark ? 'dark' : 'light') + '" href="' + storeUrl + '" target="_blank" rel="noopener noreferrer">' +
          '<div class="card-inner">' +
            '<div class="cover-wrap">' +
              '<img class="cover" src="' + this.escapeAttr(d.headerImage || '') + '" alt="' + this.escapeAttr(d.name || '') + '" loading="lazy"/>' +
            '</div>' +
            '<div class="info">' +
              '<div class="title">' + this.escapeHtml(d.name || '') + '</div>' +
              (showDesc && d.shortDescription
                ? '<div class="desc">' + this.escapeHtml(d.shortDescription) + '</div>'
                : '') +
              genresHtml +
              (metaItems ? '<div class="meta">' + metaItems + '</div>' : '') +
              personalHtml +
            '</div>' +
          '</div>' +
        '</a>';
    }

    escapeHtml(str) {
      var div = document.createElement('div');
      div.textContent = str;
      return div.innerHTML;
    }

    escapeAttr(str) {
      return str.replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }

    getStyles() {
      return '' +
        /* 重置与基础 */
        ':host { display: block; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; line-height: 1.5; container-type: inline-size; }' +
        '*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }' +

        /* 卡片容器 */
        '.card { display: block; border-radius: 8px; overflow: hidden; text-decoration: none; color: inherit; transition: box-shadow 0.2s ease, transform 0.15s ease; cursor: pointer; }' +
        '.card:hover { transform: translateY(-2px); }' +

        /* 深色主题 */
        '.card.dark { background: #1b2838; color: #c7d5e0; border: 1px solid #2a475e; box-shadow: 0 2px 8px rgba(0,0,0,0.3); }' +
        '.card.dark:hover { box-shadow: 0 6px 20px rgba(0,0,0,0.5); }' +

        /* 亮色主题 */
        '.card.light { background: #ffffff; color: #2c3e50; border: 1px solid #e0e0e0; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }' +
        '.card.light:hover { box-shadow: 0 6px 20px rgba(0,0,0,0.15); }' +

        /* 内部布局 */
        '.card-inner { display: flex; align-items: stretch; }' +

        /* 封面 */
        '.cover-wrap { width: 38%; flex-shrink: 0; overflow: hidden; background: #000; }' +
        '.cover { display: block; width: 100%; height: 100%; object-fit: cover; }' +

        /* 信息区 */
        '.info { flex: 1; padding: 16px 20px; display: flex; flex-direction: column; gap: 10px; min-width: 0; }' +

        /* 标题 */
        '.title { font-size: 18px; font-weight: 700; line-height: 1.3; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }' +
        '.card.dark .title { color: #ffffff; }' +
        '.card.light .title { color: #1a1a2e; }' +

        /* 描述 */
        '.desc { font-size: 13px; line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; opacity: 0.85; }' +

        /* 类型标签 */
        '.genres { display: flex; flex-wrap: wrap; gap: 6px; }' +
        '.genre-tag { font-size: 11px; padding: 2px 8px; border-radius: 3px; white-space: nowrap; }' +
        '.card.dark .genre-tag { background: rgba(103,194,244,0.15); color: #67c1f5; }' +
        '.card.light .genre-tag { background: #e8f4fd; color: #1a73e8; }' +

        /* 信息行 */
        '.meta { display: flex; flex-wrap: wrap; gap: 12px; margin-top: auto; padding-top: 8px; font-size: 12px; opacity: 0.75; }' +
        '.meta-item { display: inline-flex; align-items: center; gap: 4px; white-space: nowrap; }' +
        '.meta-icon { flex-shrink: 0; vertical-align: middle; }' +

        /* 个人数据 */
        '.personal-section { display: flex; flex-wrap: wrap; gap: 12px; padding-top: 8px; font-size: 12px; }' +
        '.card.dark .personal-section { border-top: 1px solid #2a475e; }' +
        '.card.light .personal-section { border-top: 1px solid #e0e0e0; }' +
        '.personal-item { display: inline-flex; align-items: center; gap: 4px; white-space: nowrap; }' +
        '.card.dark .personal-item { color: #66c0f4; }' +
        '.card.light .personal-item { color: #1a73e8; }' +

        /* 骨架屏 */
        '.skeleton-wrap { display: flex; padding: 0; min-height: 140px; }' +
        '.skeleton { border-radius: 4px; }' +
        '.card.dark .skeleton { background: linear-gradient(90deg, #2a475e 25%, #3b6a8c 50%, #2a475e 75%); background-size: 200% 100%; animation: shimmer 1.5s infinite; }' +
        '.card.light .skeleton { background: linear-gradient(90deg, #eee 25%, #ddd 50%, #eee 75%); background-size: 200% 100%; animation: shimmer 1.5s infinite; }' +
        '@keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }' +
        '.skeleton-img { width: 300px; flex-shrink: 0; border-radius: 0; }' +
        '.skeleton-content { flex: 1; padding: 16px 20px; display: flex; flex-direction: column; gap: 10px; }' +
        '.skeleton-title { height: 22px; width: 60%; }' +
        '.skeleton-text { height: 14px; width: 90%; }' +
        '.skeleton-text.short { width: 50%; }' +
        '.skeleton-tags { height: 20px; width: 40%; margin-top: auto; }' +

        /* 错误状态 */
        '.error-wrap { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 32px 20px; gap: 10px; min-height: 140px; text-align: center; }' +
        '.error-icon { opacity: 0.4; }' +
        '.error-msg { font-size: 16px; font-weight: 600; }' +
        '.error-detail { font-size: 12px; opacity: 0.6; }' +
        '.retry-btn { padding: 6px 20px; border-radius: 4px; border: none; cursor: pointer; font-size: 13px; font-weight: 500; transition: background 0.2s; }' +
        '.card.dark .retry-btn { background: #66c0f4; color: #1b2838; }' +
        '.card.dark .retry-btn:hover { background: #8ad4f8; }' +
        '.card.light .retry-btn { background: #1a73e8; color: #ffffff; }' +
        '.card.light .retry-btn:hover { background: #1565c0; }' +

        /* 响应式 - 基于容器宽度 */
        '@container (max-width: 420px) {' +
          '.card-inner { flex-direction: column; }' +
          '.cover-wrap { width: 100%; height: 180px; }' +
          '.skeleton-wrap { flex-direction: column; }' +
          '.skeleton-img { width: 100%; height: 180px; }' +
        '}' +
      '';
    }
  }

  customElements.define('steam-game-card', SteamGameCard);
})();
