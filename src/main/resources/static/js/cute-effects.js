/**
 * ASOPokeTool Cute Effects & Sound Engine
 * - Web Audio API procedural sound synthesizer (Zero external dependencies)
 * - Corner Pokemon bouncy animation & interactive tap reactions
 * - Button sparkle / electric particles & confetti celebrations
 * - Sound ON/OFF persistence
 */
(function() {
    'use strict';

    // ==========================================
    // 1. Audio Context & Sound Synthesizer
    // ==========================================
    let audioCtx = null;
    let soundEnabled = localStorage.getItem('asopoketool_sound_enabled') !== 'false';

    function getAudioContext() {
        if (!audioCtx) {
            const AudioContextClass = window.AudioContext || window.webkitAudioContext;
            if (AudioContextClass) {
                audioCtx = new AudioContextClass();
            }
        }
        if (audioCtx && audioCtx.state === 'suspended') {
            audioCtx.resume();
        }
        return audioCtx;
    }

    // 可愛らしいピカチュウ風 電撃チャイム音 (ピカッ⚡)
    function playPikachuSE() {
        if (!soundEnabled) return;
        const ctx = getAudioContext();
        if (!ctx) return;

        const now = ctx.currentTime;
        
        // 1音目: 高音のキュートなスイープ
        const osc1 = ctx.createOscillator();
        const gain1 = ctx.createGain();
        osc1.type = 'triangle';
        osc1.frequency.setValueAtTime(659.25, now); // E5
        osc1.frequency.exponentialRampToValueAtTime(1318.5, now + 0.08); // E6
        gain1.gain.setValueAtTime(0.18, now);
        gain1.gain.exponentialRampToValueAtTime(0.01, now + 0.12);
        osc1.connect(gain1);
        gain1.connect(ctx.destination);
        osc1.start(now);
        osc1.stop(now + 0.12);

        // 2音目: 弾けるピカチャイム (ピカッ！)
        const osc2 = ctx.createOscillator();
        const gain2 = ctx.createGain();
        osc2.type = 'sine';
        osc2.frequency.setValueAtTime(1760.0, now + 0.09); // A6
        osc2.frequency.exponentialRampToValueAtTime(2637.0, now + 0.18); // E7
        gain2.gain.setValueAtTime(0.22, now + 0.09);
        gain2.gain.exponentialRampToValueAtTime(0.001, now + 0.28);
        osc2.connect(gain2);
        gain2.connect(ctx.destination);
        osc2.start(now + 0.09);
        osc2.stop(now + 0.28);
    }

    // ポップなUIタップ音 (ポコッ♪)
    function playPopSE() {
        if (!soundEnabled) return;
        const ctx = getAudioContext();
        if (!ctx) return;

        const now = ctx.currentTime;
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();

        osc.type = 'sine';
        osc.frequency.setValueAtTime(440, now);
        osc.frequency.exponentialRampToValueAtTime(880, now + 0.06);

        gain.gain.setValueAtTime(0.15, now);
        gain.gain.exponentialRampToValueAtTime(0.001, now + 0.08);

        osc.connect(gain);
        gain.connect(ctx.destination);

        osc.start(now);
        osc.stop(now + 0.08);
    }

    // ゼニガメの水しぶき音 (ポチャッ💧)
    function playSquirtleSE() {
        if (!soundEnabled) return;
        const ctx = getAudioContext();
        if (!ctx) return;

        const now = ctx.currentTime;
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();

        osc.type = 'sine';
        osc.frequency.setValueAtTime(800, now);
        osc.frequency.exponentialRampToValueAtTime(400, now + 0.08);
        osc.frequency.exponentialRampToValueAtTime(1200, now + 0.15);

        gain.gain.setValueAtTime(0.18, now);
        gain.gain.exponentialRampToValueAtTime(0.001, now + 0.18);

        osc.connect(gain);
        gain.connect(ctx.destination);

        osc.start(now);
        osc.stop(now + 0.18);
    }

    // ヒトカゲのパチパチ炎音 (ポポッ🔥)
    function playCharmanderSE() {
        if (!soundEnabled) return;
        const ctx = getAudioContext();
        if (!ctx) return;

        const now = ctx.currentTime;
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();

        osc.type = 'sawtooth';
        osc.frequency.setValueAtTime(523.25, now); // C5
        osc.frequency.exponentialRampToValueAtTime(1046.5, now + 0.1); // C6

        gain.gain.setValueAtTime(0.12, now);
        gain.gain.exponentialRampToValueAtTime(0.001, now + 0.14);

        osc.connect(gain);
        gain.connect(ctx.destination);

        osc.start(now);
        osc.stop(now + 0.14);
    }

    // フシギダネのキラキラ草音 (ティロン🍃)
    function playBulbasaurSE() {
        if (!soundEnabled) return;
        const ctx = getAudioContext();
        if (!ctx) return;

        const now = ctx.currentTime;
        [523.25, 659.25, 783.99].forEach((freq, idx) => {
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();
            const t = now + idx * 0.05;

            osc.type = 'triangle';
            osc.frequency.setValueAtTime(freq, t);

            gain.gain.setValueAtTime(0.15, t);
            gain.gain.exponentialRampToValueAtTime(0.001, t + 0.12);

            osc.connect(gain);
            gain.connect(ctx.destination);

            osc.start(t);
            osc.stop(t + 0.12);
        });
    }

    // 勝利・お祝いファンファーレ (パパパパーン！🎉)
    function playFanfareSE() {
        if (!soundEnabled) return;
        const ctx = getAudioContext();
        if (!ctx) return;

        const now = ctx.currentTime;
        const notes = [
            { f: 523.25, t: 0.00, d: 0.10 }, // C5
            { f: 659.25, t: 0.10, d: 0.10 }, // E5
            { f: 783.99, t: 0.20, d: 0.10 }, // G5
            { f: 1046.5, t: 0.30, d: 0.35 }  // C6
        ];

        notes.forEach(note => {
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();
            const startTime = now + note.t;

            osc.type = 'triangle';
            osc.frequency.setValueAtTime(note.f, startTime);

            gain.gain.setValueAtTime(0.2, startTime);
            gain.gain.exponentialRampToValueAtTime(0.001, startTime + note.d);

            osc.connect(gain);
            gain.connect(ctx.destination);

            osc.start(startTime);
            osc.stop(startTime + note.d);
        });
    }

    // ==========================================
    // 2. Particle & Confetti Engine
    // ==========================================
    function spawnCuteParticles(x, y, emojiList, count = 8) {
        for (let i = 0; i < count; i++) {
            const p = document.createElement('div');
            p.className = 'cute-particle';
            p.textContent = emojiList[Math.floor(Math.random() * emojiList.length)];
            
            const angle = (Math.PI * 2 / count) * i + (Math.random() * 0.4 - 0.2);
            const distance = 40 + Math.random() * 55;
            const tx = Math.cos(angle) * distance + 'px';
            const ty = Math.sin(angle) * distance + 'px';
            const rot = (Math.random() * 120 - 60) + 'deg';

            p.style.left = x + 'px';
            p.style.top = y + 'px';
            p.style.setProperty('--tx', tx);
            p.style.setProperty('--ty', ty);
            p.style.setProperty('--rot', rot);

            document.body.appendChild(p);

            setTimeout(() => {
                p.remove();
            }, 850);
        }
    }

    function spawnConfetti(count = 50) {
        const colors = ['#FF6B35', '#FFD166', '#06D6A0', '#118AB2', '#EF476F', '#9D4EDD', '#FFB6C1'];
        for (let i = 0; i < count; i++) {
            const c = document.createElement('div');
            c.className = 'confetti-piece';
            c.style.backgroundColor = colors[Math.floor(Math.random() * colors.length)];
            
            const startX = Math.random() * 100 + 'vw';
            const endX = (parseFloat(startX) + (Math.random() * 20 - 10)) + 'vw';
            
            c.style.setProperty('--sx', startX);
            c.style.setProperty('--ex', endX);
            c.style.left = '0';
            c.style.top = '0';
            c.style.animationDelay = (Math.random() * 0.6) + 's';
            c.style.animationDuration = (2.0 + Math.random() * 1.5) + 's';

            document.body.appendChild(c);

            setTimeout(() => {
                c.remove();
            }, 4000);
        }
    }

    // ==========================================
    // 3. Setup Interactivity
    // ==========================================
    function setupInteractions() {
        // --- 🌸 四隅のポケモンのクリックリアクション ---
        const cornerPokemons = document.querySelectorAll('.pokemon-corner');
        cornerPokemons.forEach(img => {
            img.addEventListener('click', function(e) {
                const rect = img.getBoundingClientRect();
                const centerX = rect.left + rect.width / 2;
                const centerY = rect.top + rect.height / 2;

                // ジャンプアニメーション発動
                img.classList.remove('poke-jumping');
                void img.offsetWidth; // reflow
                img.classList.add('poke-jumping');

                const src = (img.getAttribute('src') || '') + (img.getAttribute('alt') || '');

                if (src.includes('pikachu') || src.includes('ピカチュウ')) {
                    playPikachuSE();
                    spawnCuteParticles(centerX, centerY, ['⚡', '✨', '⭐', '💛', '🌟', '⚡']);
                } else if (src.includes('bulbasaur') || src.includes('フシギダネ')) {
                    playBulbasaurSE();
                    spawnCuteParticles(centerX, centerY, ['🍃', '🌿', '✨', '💚', '🌸', '🌱']);
                } else if (src.includes('charmander') || src.includes('ヒトカゲ')) {
                    playCharmanderSE();
                    spawnCuteParticles(centerX, centerY, ['🔥', '✨', '💥', '🧡', '⭐', '🔥']);
                } else if (src.includes('squirtle') || src.includes('ゼニガメ')) {
                    playSquirtleSE();
                    spawnCuteParticles(centerX, centerY, ['💧', '🫧', '✨', '💙', '🌊', '💧']);
                } else {
                    playPikachuSE();
                    spawnCuteParticles(centerX, centerY, ['✨', '💖', '⭐', '🌟']);
                }
            });
        });

        // --- ⚡ ボタン・対戦カードのクリックエフェクト ---
        const clickableSelector = '.btn-primary-custom, .btn-secondary-custom, .project-match-card.clickable, .bracket-match-box.clickable, .btn-electric-click';
        document.addEventListener('click', function(e) {
            const target = e.target.closest(clickableSelector);
            if (!target) return;

            // 四隅のポケモン自身やサウンド切替ボタンの場合はスキップ
            if (target.classList.contains('pokemon-corner') || target.closest('#cuteSoundToggleBtn')) return;

            const x = e.clientX || (target.getBoundingClientRect().left + target.getBoundingClientRect().width / 2);
            const y = e.clientY || (target.getBoundingClientRect().top + target.getBoundingClientRect().height / 2);

            playPopSE();
            spawnCuteParticles(x, y, ['✨', '⚡', '⭐', '💛'], 5);
        }, true);

        // --- 🔊 サウンド切替ボタンの設置（画面最下部中央・小型ピル型） ---
        if (!document.getElementById('cuteSoundToggleBtn')) {
            const toggleBtn = document.createElement('button');
            toggleBtn.id = 'cuteSoundToggleBtn';
            
            function updateToggleContent() {
                if (soundEnabled) {
                    toggleBtn.className = 'sound-toggle-btn';
                    toggleBtn.innerHTML = '<i class="fa-solid fa-volume-high"></i> <span>効果音 ON</span>';
                    toggleBtn.setAttribute('title', '効果音: ON (クリックでOFF)');
                } else {
                    toggleBtn.className = 'sound-toggle-btn muted';
                    toggleBtn.innerHTML = '<i class="fa-solid fa-volume-xmark"></i> <span>効果音 OFF</span>';
                    toggleBtn.setAttribute('title', '効果音: OFF (クリックでON)');
                }
            }
            updateToggleContent();

            toggleBtn.addEventListener('click', function(e) {
                e.stopPropagation();
                soundEnabled = !soundEnabled;
                localStorage.setItem('asopoketool_sound_enabled', soundEnabled ? 'true' : 'false');
                updateToggleContent();

                if (soundEnabled) {
                    playPikachuSE();
                    const rect = toggleBtn.getBoundingClientRect();
                    spawnCuteParticles(rect.left + rect.width / 2, rect.top + rect.height / 2, ['🔊', '⚡', '✨', '💖'], 6);
                }
            });

            document.body.appendChild(toggleBtn);
        }
    }

    // DOM読み込み完了時に初期化
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', setupInteractions);
    } else {
        setupInteractions();
    }

    // グローバル公開 (他スクリプトから紙吹雪やファンファーレを呼べるようにする)
    window.CuteEffects = {
        playPikachuSE: playPikachuSE,
        playPopSE: playPopSE,
        playFanfareSE: playFanfareSE,
        spawnParticles: spawnCuteParticles,
        spawnConfetti: spawnConfetti
    };
})();
