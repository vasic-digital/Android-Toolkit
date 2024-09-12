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
include ':Toolkit:FastscrollerAlphabet'
```

- And, add the modules to your `build.gradle` as the dependencies (the ones that you are going to use):

```groovy
dependencies {

    implementation project(":Toolkit:Main")
    implementation project(":Toolkit:Test")
    implementation project(":Toolkit:Access")
    implementation project(":Toolkit:RootShell")
    implementation project(":Toolkit:RootTools")
    implementation project(':Toolkit:Interprocess')

    testImplementation project(":Toolkit:Main")
    testImplementation project(":Toolkit:Test")

    androidTestImplementation project(":Toolkit:Main")
    androidTestImplementation project(":Toolkit:Test")
}
```

That is all. You are ready to use the `Android Toolkit`!