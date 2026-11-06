# MyIMS (My Input Method Service)

一个精简的 Android 中文输入法应用，作为 [librime](https://github.com/rime/librime) 的前端实现。

## 项目特点

- **轻量精简**：没有复杂繁琐的功能，专注于满足个人的日常中文输入需求。
- **现代化 UI**：UI 层完全使用 Jetpack Compose 构建，界面简洁现代。
- **Rime 核心**：底层通过 `rime-android` 模块（基于 librime）提供强大的中州韵输入法引擎支持。
- **当前进度**：目前已完成虚拟键盘（Virtual Keyboard）核心部分，物理键盘支持将在后续版本中持续改进。

## 配置与词库

- 项目内置的 `assets/rime.zip` 默认配置了**白霜拼音**。
- 如果您有自己的个性化配置，可以通过 SAF（Storage Access Framework）将配置文件放入 `rime/` 目录下，随后进行重新部署即可。

## 模块结构

- `:app`：输入法应用主模块（UI、IME Service 等，采用 GPL-3.0 许可证）。
- `:rime-android`：Rime JNI 封装（采用 Apache-2.0 许可证）。

## 许可证

本项目各模块采用不同的开源许可证：
- `app` 模块遵循 [GPL-3.0 许可证](app/LICENSE)。
- `rime-android` 模块遵循 [Apache-2.0 许可证](rime-android/LICENSE)。
