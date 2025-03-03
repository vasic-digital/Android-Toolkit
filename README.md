# Android Toolkit

Commonly used set of abstractions, implementations and tools.

## How to use

- `cd` into your Android project's root and execute:

```bash
git submodule add "GIT_REPO_URL" ./Toolkit  
```

- Add the following to your `settings.gradle` (only the modules that you need from this list):

```groovy
include ':Toolkit:Main'
include ':Toolkit:Test'
include ':Toolkit:Echo'
include ':Toolkit:Access'
include ':Toolkit:JCommons'
include ':Toolkit:RootTools'
include ':Toolkit:RootShell'
include ':Toolkit:Interprocess'
include ':Toolkit:CircleImageView'
include ':Toolkit:ConnectionIndicator'
include ':Toolkit:FastscrollerAlphabet'
```

- And, add the modules to your `build.gradle` as the dependencies (the ones that you are going to use):

```groovy
dependencies {

    implementation project("${toolkit_context}:Main")
    implementation project("${toolkit_context}:Test")
    implementation project("${toolkit_context}:Access")
    implementation project("${toolkit_context}:RootShell")
    implementation project("${toolkit_context}:RootTools")
    implementation project(':Toolkit:Interprocess')

    testImplementation project("${toolkit_context}:Main")
    testImplementation project("${toolkit_context}:Test")

    androidTestImplementation project("${toolkit_context}:Main")
    androidTestImplementation project("${toolkit_context}:Test")
}
```

That is all. You are ready to use the `Android Toolkit`!