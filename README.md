-- WezTerm master config (Windows) - PowerShell 风格快捷键 + One Half Dark
local wezterm = require 'wezterm'

-- 背景图路径（自己换）
local bg_img = 'C:\\Users\\' .. os.getenv('USERNAME') .. '\\Pictures\\ShellBackground1.jpg'

local act = wezterm.action

-- 复制功能：有选中则复制，无选中则发送 Ctrl+C（中断命令）
local function copy_or_interrupt(window, pane)
    local has_selection = window:get_selection_text_for_pane(pane) ~= ""
    if has_selection then
        window:perform_action(act.CopyTo('Clipboard'), pane)
        window:perform_action(act.ClearSelection, pane)
    else
        window:perform_action(act.SendKey{ key = 'c', mods = 'CTRL' }, pane)
    end
end

-- 粘贴功能：有选中则先删除选中再粘贴，无选中则直接粘贴
local function smart_paste(window, pane)
    local has_selection = window:get_selection_text_for_pane(pane) ~= ""
    if has_selection then
        -- 有选中文本：先发送 Delete 删除选中，再粘贴
        window:perform_action(act.SendKey{ key = 'Delete', mods = 'NONE' }, pane)
        window:perform_action(act.PasteFrom('Clipboard'), pane)
    else
        -- 无选中文本：直接粘贴
        window:perform_action(act.PasteFrom('Clipboard'), pane)
    end
end

local config = {
    -- 1. 字体：One Half 家族 + Nerd-Font 图标回退
    font = wezterm.font_with_fallback {
        { family = 'FiraCode Nerd Font', weight = 'Regular' },
    },

    font_size = 14.0,
    initial_cols = 137,
    initial_rows = 38,
    
    -- 2. 颜色主题
    color_scheme = 'OneHalfDark',

    -- 3. 亚克力 + 背景图
    win32_system_backdrop = 'Acrylic',

    background = {
        {
            source = { File = bg_img },
            opacity = 0.2,
            horizontal_align = 'Center',
            vertical_align   = 'Middle',
            repeat_x = 'NoRepeat',
            repeat_y = 'NoRepeat',
        },
    },
    window_background_opacity = 1,

    -- 4. 启动时默认标签
    default_prog = { 'pwsh', '-NoLogo' },
    launch_menu = {
        { label = 'PowerShell 7',      args = { 'pwsh', '-NoLogo' } },
        { label = 'Windows PowerShell',args = { 'powershell', '-NoLogo' } },
        { label = 'WSL (Ubuntu)',      args = { 'wsl.exe', '~' } },
    },

    -- 5. 窗口装饰与行为
    window_decorations = 'TITLE | RESIZE',
    hide_tab_bar_if_only_one_tab = true,
    window_close_confirmation = 'NeverPrompt',
    adjust_window_size_when_changing_font_size = false,

    window_padding = {
        left   = 0,
        right  = 0,
        top    = 0,
        bottom = 0,
    },

    -- 6. 标题相关
    set_environment_variables = {
        WEZTERM_SHELL_TITLE = 'true',
    },

    -- ==================== One Half Dark 自定义微调 ====================
    colors = {
        -- 选中高亮：白底黑字
        selection_bg = '#ffffff',
        selection_fg = '#000000',
        
        -- 光标：竖线，白边框 + 黑色内部实现渐变效果
        cursor_bg = '#000000',
        cursor_fg = '#ffffff',
        cursor_border = '#ffffff',
        
        tab_bar = {
            background = '#282c34',
            active_tab = { bg_color = '#61afef', fg_color = '#282c34' },
            inactive_tab = { bg_color = '#3e4451', fg_color = '#abb2bf' },
            inactive_tab_hover = { bg_color = '#4b5263', fg_color = '#dcdfe4' },
            new_tab = { bg_color = '#282c34', fg_color = '#61afef' },
            new_tab_hover = { bg_color = '#3e4451', fg_color = '#61afef' },
        },
        split = '#3e4451',
        scrollbar_thumb = '#4b5263',
    },

    -- 光标样式：闪烁竖线
    default_cursor_style = 'BlinkingBar',
    cursor_blink_rate = 800,

    -- ==================== PowerShell 风格快捷键 ====================
    keys = {
        -- 复制：智能判断（有选中则复制，无则中断）
        { key = 'c', mods = 'CTRL', action = wezterm.action_callback(copy_or_interrupt) },
        
        -- 粘贴：智能粘贴（有选中则替换，无则直接粘贴）
        { key = 'v', mods = 'CTRL', action = wezterm.action_callback(smart_paste) },
        { key = 'v', mods = 'CTRL|SHIFT', action = wezterm.action_callback(smart_paste) },
        
        -- 全选（发送给 PowerShell 处理）
        { key = 'a', mods = 'CTRL|SHIFT', action = act.SendKey{ key = 'a', mods = 'CTRL|SHIFT' } },
        
        -- 新建标签页
        { key = 't', mods = 'CTRL|SHIFT', action = act.SpawnTab('CurrentPaneDomain') },
        
        -- 关闭标签
        { key = 'w', mods = 'CTRL|SHIFT', action = act.CloseCurrentTab{confirm=false} },
        
        -- 切换标签
        { key = 'Tab', mods = 'CTRL', action = act.ActivateTabRelative(1) },
        { key = 'Tab', mods = 'CTRL|SHIFT', action = act.ActivateTabRelative(-1) },
        
        -- 直接跳转标签 Ctrl+1~9
        { key = '1', mods = 'CTRL', action = act.ActivateTab(0) },
        { key = '2', mods = 'CTRL', action = act.ActivateTab(1) },
        { key = '3', mods = 'CTRL', action = act.ActivateTab(2) },
        { key = '4', mods = 'CTRL', action = act.ActivateTab(3) },
        { key = '5', mods = 'CTRL', action = act.ActivateTab(4) },
        { key = '6', mods = 'CTRL', action = act.ActivateTab(5) },
        { key = '7', mods = 'CTRL', action = act.ActivateTab(6) },
        { key = '8', mods = 'CTRL', action = act.ActivateTab(7) },
        { key = '9', mods = 'CTRL', action = act.ActivateTab(8) },
        
        -- 全屏
        { key = 'F11', mods = 'NONE', action = act.ToggleFullScreen },
        
        -- 字体缩放
        { key = '=', mods = 'CTRL', action = act.IncreaseFontSize },
        { key = '-', mods = 'CTRL', action = act.DecreaseFontSize },
        { key = '0', mods = 'CTRL', action = act.ResetFontSize },
        
        -- 清屏：发送 clear 命令（像 Linux 的 clear，不清除滚动历史）
        { key = 'l', mods = 'CTRL', action = act.SendKey{ key = 'l', mods = 'CTRL' } },
        
        -- 查找（Ctrl+Shift+F）
        { key = 'f', mods = 'CTRL|SHIFT', action = act.Search{CaseInSensitiveString=''} },
        
        -- 窗口分割
        { key = '%', mods = 'CTRL|SHIFT', action = act.SplitHorizontal{domain='CurrentPaneDomain'} },
        { key = '"', mods = 'CTRL|SHIFT', action = act.SplitVertical{domain='CurrentPaneDomain'} },
        
        -- 切换窗格（Alt+方向键）
        { key = 'LeftArrow', mods = 'ALT', action = act.ActivatePaneDirection('Left') },
        { key = 'RightArrow', mods = 'ALT', action = act.ActivatePaneDirection('Right') },
        { key = 'UpArrow', mods = 'ALT', action = act.ActivatePaneDirection('Up') },
        { key = 'DownArrow', mods = 'ALT', action = act.ActivatePaneDirection('Down') },
        
        -- 调整窗格大小
        { key = 'LeftArrow', mods = 'ALT|SHIFT', action = act.AdjustPaneSize{'Left', 3} },
        { key = 'RightArrow', mods = 'ALT|SHIFT', action = act.AdjustPaneSize{'Right', 3} },
        { key = 'UpArrow', mods = 'ALT|SHIFT', action = act.AdjustPaneSize{'Up', 3} },
        { key = 'DownArrow', mods = 'ALT|SHIFT', action = act.AdjustPaneSize{'Down', 3} },
        
        -- 新建窗口
        { key = 'n', mods = 'CTRL|SHIFT', action = act.SpawnWindow },
        
        -- 关闭窗格
        { key = 'w', mods = 'CTRL|ALT', action = act.CloseCurrentPane{confirm=false} },
        
        -- 切换全屏窗格
        { key = 'Enter', mods = 'ALT', action = act.TogglePaneZoomState },
        
        -- 快速编辑配置文件
        { key = ',', mods = 'CTRL|SHIFT', action = act.SpawnCommandInNewWindow{
            args = {'notepad.exe', wezterm.config_file},
        }},
    },

    -- ==================== 鼠标绑定 ====================
    mouse_bindings = {
        -- 右键粘贴（智能替换）
        {
            event = { Down = { streak = 1, button = 'Right' } },
            mods = 'NONE',
            action = wezterm.action_callback(smart_paste),
        },
        -- Shift+右键 暂时无操作
        {
            event = { Down = { streak = 1, button = 'Right' } },
            mods = 'SHIFT',
            action = act.Nop,
        },
        -- 选中自动复制
        {
            event = { Up = { streak = 1, button = 'Left' } },
            mods = 'NONE',
            action = act.CompleteSelection('ClipboardAndPrimarySelection'),
        },
        -- Ctrl+点击 打开链接
        {
            event = { Up = { streak = 1, button = 'Left' } },
            mods = 'CTRL',
            action = act.OpenLinkAtMouseCursor,
        },
    },

    -- 滚动设置
    scrollback_lines = 10000,
    enable_scroll_bar = true,

    -- 其他
    hide_mouse_cursor_when_typing = true,
    automatically_reload_config = true,
}

-- 默认 WSL 配置
config.default_domain = 'WSL:Ubuntu'
config.default_cwd = '~'

-- 8. 动态标题格式化
wezterm.on('format-tab-title', function(tab, _tabs, _panes, _config, _hover, _max_width)
    local pane = tab.active_pane
    local title = pane.title

    local prefix
    if pane.domain_name == 'local' and pane.foreground_process_name:find('pwsh') then
        prefix = 'WezTerm ❯ PowerShell'
    elseif pane.domain_name == 'local' and pane.foreground_process_name:find('wsl') then
        prefix = 'WezTerm ❯ Ubuntu'
    else
        prefix = 'WezTerm'
    end

    local suffix = ''
    if title and title ~= '' and title ~= prefix then
        suffix = ': ' .. title
    end

    return prefix .. suffix
end)

return config