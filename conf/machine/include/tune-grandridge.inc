DEFAULTTUNE ??= "grandridge-64"

require conf/machine/include/x86/tune-corei7.inc

TUNEVALID[grandridge] = "Enable Grand Ridge specific processor optimizations"

TUNE_CCARGS .= "${@bb.utils.contains("TUNE_FEATURES", "grandridge", " -march=grandridge -mfpmath=sse", "", d)}"

AVAILTUNES += "grandridge-32 grandridge-64"
TUNE_FEATURES:tune-grandridge-32 = "${TUNE_FEATURES:tune-x86} grandridge"
TUNE_FEATURES:tune-grandridge-64 = "${TUNE_FEATURES:tune-x86-64} grandridge"

BASE_LIB:tune-grandridge-32 = "lib"
BASE_LIB:tune-grandridge-64 = "lib64"

TUNE_PKGARCH:tune-grandridge-32 = "grandridge-32"
TUNE_PKGARCH:tune-grandridge-64 = "grandridge-64"

PACKAGE_EXTRA_ARCHS:tune-grandridge-32 = "${PACKAGE_EXTRA_ARCHS:tune-x86} grandridge-32"
PACKAGE_EXTRA_ARCHS:tune-grandridge-64 = "${PACKAGE_EXTRA_ARCHS:tune-x86-64} grandridge-64"
