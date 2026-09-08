# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\Ibrahim\AppData\Local\Android\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# AppFunctions (androidx.appfunctions alpha10). KSP generates assets/root_app_function_service.xml
# BEFORE R8 runs, and it identifies every function by its declaring class + method name
# (com.iboalali.basicrootchecker.appfunctions.BaseRootAppFunctionService#checkRootStatus). The
# library's consumer rules keep the @AppFunctionSerializable models and the generated inventory, but
# NOT the @AppFunctionServiceEntryPoint host: R8 vertically merged BaseRootAppFunctionService into
# the generated RootAppFunctionService subclass, so the class named in the XML was absent from the
# release DEX. Play rejects an AAB whose function XML doesn't resolve against the binary ("The
# Android App Functions XML could not be parsed from the binary"). Keep the host class name and its
# @AppFunction method names so the XML matches.
-keepclasseswithmembers class * {
    @androidx.appfunctions.AppFunction <methods>;
}
