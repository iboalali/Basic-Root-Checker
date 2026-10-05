# AppFunctions. KSP generates assets/root_app_function_service.xml
# BEFORE R8 runs, and it identifies every function by its declaring class + method name
# (com.iboalali.basicrootchecker.appfunctions.BaseRootAppFunctionService#checkRootStatus). The
# library's consumer rules keep the @AppFunctionSerializable models and the generated inventory, but
# NOT the @AppFunctionServiceEntryPoint host. Without this rule R8 vertically merges
# BaseRootAppFunctionService into the generated RootAppFunctionService subclass, so the class named
# in the XML is absent from the release DEX. Play rejects an AAB whose function XML doesn't resolve against the binary ("The
# Android App Functions XML could not be parsed from the binary"). Keep the host class name and its
# @AppFunction method names so the XML matches.
-keepclasseswithmembers class * {
    @androidx.appfunctions.AppFunction <methods>;
}
