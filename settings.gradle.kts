pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    // 用 PREFER_SETTINGS 而非 FAIL_ON_PROJECT_REPOS：Kotlin/Wasm 插件需要在项目层添加
    // Node.js 分发仓库（https://nodejs.org/dist）来下载 Node，严格模式会直接拒绝它。
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        // Kotlin/Wasm 下载 Node.js 分发所需的 Ivy 仓库（org.nodejs:node）
        ivy {
            name = "Node Distributions at https://nodejs.org/dist"
            url = uri("https://nodejs.org/dist/")
            patternLayout {
                artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]")
            }
            metadataSources { artifact() }
            content { includeModule("org.nodejs", "node") }
        }
        // Kotlin/Wasm 下载 Yarn 分发所需的 Ivy 仓库（com.yarnpkg:yarn）
        ivy {
            name = "Yarn Distributions at https://github.com/yarnpkg/yarn/releases/download"
            url = uri("https://github.com/yarnpkg/yarn/releases/download/")
            patternLayout {
                artifact("v[revision]/[artifact](-v[revision]).[ext]")
            }
            metadataSources { artifact() }
            content { includeModule("com.yarnpkg", "yarn") }
        }
        // Kotlin/Wasm 下载 Binaryen 分发所需的 Ivy 仓库（com.github.webassembly:binaryen）
        ivy {
            name = "Binaryen Distributions at https://github.com/WebAssembly/binaryen/releases/download"
            url = uri("https://github.com/WebAssembly/binaryen/releases/download/")
            patternLayout {
                artifact("version_[revision]/[artifact]-version_[revision]-[classifier].[ext]")
            }
            metadataSources { artifact() }
            content { includeModule("com.github.webassembly", "binaryen") }
        }
    }
}

rootProject.name = "CocHelper"
include(":composeApp")

// 网页部署 CI（WEB_ONLY=true）只需 wasmJs 的 :composeApp；
// :app 是 Android 模块，Gradle 配置期就需要本地 Android SDK，CI 上跳过以免 AGP 报错。
if (System.getenv("WEB_ONLY") != "true") {
    include(":app")
}
