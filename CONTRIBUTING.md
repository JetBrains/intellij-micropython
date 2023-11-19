# Support for different platforms

We support some latest releases of platform (for example, 2023.2 and 2023.3).
To be sure the plugin works with all releases, we have to build plugin with different versions of IntelliJ platform.
The plugin project has separate settings for each platform version (in general, IDE and plugin versions)
to avoid manual changes when you want to compile project with non default platform version.
These settings are stored in `gradle-%platform.version%.properties` files.

Sometimes there are incompatible changes in a new platform version.
To avoid creating several parallel vcs branches for each version,
we use conditional compilation based on Gradle [source sets](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.SourceSet.html)
to keep platform-specific sources in separate directories and use only necessary ones for compilation.

For example, `com.jetbrains.micropython.repl.Terminal#reset` function
should have separate implementations for 232 and 233 platforms.
Let's consider that the function is placed in `compatUtils.kt` file.
Then source code structure will be

     +-- intelij-micropython
     |   +-- src/232/main
     |       +-- com/jetbrains/micropython/repl
     |           +-- compatUtils.kt
     |   +-- src/233/main
     |       +-- com/jetbrains/micropython/repl
     |           +-- compatUtils.kt
     |   +-- src/main
     |       +-- other platfrom independent code

Of course, only one batch of platform-specific code can be used in compilation.
To change platform version which you use during compilation,
i.e. change IDE and plugin dependencies, and platform dependent code,
you need to modify `platformVersion` property in `gradle.properties` file or
pass `-PplatformVersion=%platform.version%` argument to Gradle command.

See [Tips and tricks](#tips-and-tricks) section to get more details how to create platform-specific code.

#### How to support new platform version

The following explanation uses `old`, `current` and `new` platform terms that mean:
* `old` - number of the oldest supported platform version that should be dropped
* `current` - number of the latest major stable platform release
* `new` - number of new platform that should be supported

For example, at the moment of writing, `231` is `old`, `232` is `current` and `233` is `new`.
See [build_number_ranges](https://jetbrains.org/intellij/sdk/docs/basics/getting_started/build_number_ranges.html) for more info about platform versions.

Step-by-step instruction on how to support new platform version:
* Drop all code related to the `old` platform version, i.e. drop `gradle-%old%.properties` and `src/%old%` directory
* Move code from `src/%current%` directory into common source set,
  i.e. `src/%current%/main` into `src/main` and `src/%current%/test` into `src/test`.
  Also, simplify the moved code if possible.
* Add support for `new` platform, i.e. add `gradle-%new%.properties` with all necessary properties and make it compile.
  It may be required to extract some code into platform-specific source sets to make plugin compile with each supported platform.
  See [Tips and tricks](#tips-and-tricks) section for the most common examples of how to do it
* Fix tests if needed
* Update log path in `runIde` run configuration, i.e:
    - drop `idea-%old%.log` item
    - add `idea-%new%.log` item with `$PROJECT_DIR$/plugin/build/idea-sandbox-%new%/system/log/idea.log` path
* Update CI workflows to use new platform version instead of old one.
  It usually requires just to update `platform-version` list in all workflows where we build the plugin
* Fix `BACKCOMPAT: %old%` comments

#### Tips and tricks

A non-exhaustive list of tips how you can adapt your code for several platforms:
* if you need to execute specific code for each platform in gradle build scripts (`build.gradle.kts` or `settings.gradle.kts`),
  just use `platformVersion` property and `if`/`when` conditions.
  Note, in `build.gradle.kts` value of this property is already retrieved into `platformVersion` variable
* if you need to have different sources for each platform:
    - check that you actually need to have specific code for each platform.
      There is quite often a deprecated way to make necessary action.
      If it's your case, don't forget to suppress the deprecation warning and add `// BACKCOMPAT: %current%` comment to mark this code and
      fix the deprecation in the future
    - extract different code into a function and place it into `compatUtils.kt` file in each platform-specific source set.
      It usually works well when you need to call specific public code to make the same things in each platform
    - if code that you need to call is not public (for example, it uses some protected methods of parent class), use the inheritance mechanism.
      Extract `AwesomeClassBase` from your `AwesomeClass`, inherit `AwesomeClass` from `AwesomeClassBase`,
      move `AwesomeClassBase` into platform specific source sets and move all platform specific code into `AwesomeClassBase` as protected methods.
    - sometimes, signatures of some methods can be specified while platform evolution.
      For example, `protected abstract void foo(Bar<Baz> bar)` can be converted into `protected abstract void foo(Bar<? extends Baz> bar)`,
      and you have to override this method in your implementation.
      It introduces source incompatibility (although it doesn't break binary compatibility).
      The simplest way to make it compile for each platform is to introduce platform-specific [`typealias`](https://kotlinlang.org/docs/reference/type-aliases.html),
      i.e. `typealias PlaformBar = Bar<Baz>` for `current` platform and `typealias PlaformBar = Bar<out Baz>` for `new` one and use it in signature of overridden method.
      Also, this approach works well when some class you depend on was moved into another package.
    - if creation of platform-specific source set is too heavy for your case, there is a way how you can choose specific code in runtime.
      Just create the corresponding condition using `com.intellij.openapi.application.ApplicationInfo.getBuild`.
      Usually, it looks like
      ```kotlin
      val BUILD_%new% = BuildNumber.fromString("%new%")!!
      if (ApplicationInfo.getInstance().build < BUILD_%new%) {
          // code for current platform
      } else {
          // code for new platform
      }
      ```
      Of course, code should be compilable with all supported platforms to use this approach.
    - sometimes you want to disable some tests temporarily to find out why they don't work later.
* if you need to register different items in `plugin.xml` for each platform:
    1. create `platform-plugin.xml` in `src/%current%/main/resources/META-INF` and `src/%new%/main/resources/META-INF`
    2. put platform specific definitions into these xml files
    3. include platform specific xml into `src/main/resources/META-INF/plugin.xml`, i.e. add the following code
    ```xml
      <idea-plugin xmlns:xi="http://www.w3.org/2001/XInclude">
          <xi:include href="/META-INF/platform-plugin.xml" xpointer="xpointer(/idea-plugin/*)"/>
      </idea-plugin>  
    ```
