#!/bin/bash

# ==============================================================================
# Zenoh Swarm Bus Client — Developer Dependency & Compiler Setup Checker
# ==============================================================================
# This scripts validates that your local development machine contains the minimum
# required elements to build and compile the Zenoh Native JNI applet.
# ==============================================================================

BOLD='\033[1m'
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

clear
echo -e "${BOLD}${CYAN}======================================================================${NC}"
echo -e "${BOLD}${CYAN}               ZENOH SWARM BUS CLIENT SETUP CHECKER                   ${NC}"
echo -e "${BOLD}${CYAN}======================================================================${NC}"
echo "This helper evaluates your system specs for compiling the android-zenoh app."
echo ""

errors=0
warnings=0

# 1. Evaluate Java SDK Environment
echo -e "${BOLD}[1/5] Checking Java Development Kit (JDK)...${NC}"
if command -v java >/dev/null 2>&1; then
    java_ver=$(java -version 2>&1 | head -n 1)
    echo -e "  ${GREEN}✓ Java is installed:${NC} $java_ver"
else
    echo -e "  ${RED}✗ Java (JDK) not found${NC}"
    echo "    -> Action: Please install JDK 17 or higher (recommended for Gradle 8.x)."
    ((errors++))
fi

if [ -n "$JAVA_HOME" ]; then
    echo -e "  ${GREEN}✓ JAVA_HOME is configured:${NC} $JAVA_HOME"
else
    echo -e "  ${YELLOW}⚠ JAVA_HOME environment variable is not defined${NC}"
    echo "    -> Action: Configure JAVA_HOME to point to your JDK directory."
    ((warnings++))
fi
echo ""

# 2. Evaluate Android Home Path environment variables
echo -e "${BOLD}[2/5] Checking Android SDK Home (ANDROID_HOME)...${NC}"
if [ -n "$ANDROID_HOME" ]; then
    echo -e "  ${GREEN}✓ ANDROID_HOME is defined:${NC} $ANDROID_HOME"
    if [ -d "$ANDROID_HOME/platforms" ] && [ -d "$ANDROID_HOME/platform-tools" ]; then
        echo -e "  ${GREEN}✓ Android platforms/tools directories validated.${NC}"
    else
        echo -e "  ${YELLOW}⚠ Paths under ANDROID_HOME are missing platforms/platform-tools folders.${NC}"
        ((warnings++))
    fi
else
    echo -e "  ${RED}✗ ANDROID_HOME environment variable is missing.${NC}"
    echo "    -> Action: Export ANDROID_HOME directing to your local Android SDK location."
    ((errors++))
fi
echo ""

# 3. Evaluate Gradle Engine versioning
echo -e "${BOLD}[3/5] Checking Gradle installation...${NC}"
if command -v gradle >/dev/null 2>&1; then
    gradle_ver=$(gradle -v | grep "Gradle" | head -n 1)
    echo -e "  ${GREEN}✓ Gradle system launcher is installed:${NC} $gradle_ver"
else
    echo -e "  ${YELLOW}⚠ System-wide 'gradle' installation not located.${NC}"
    echo "    -> Note: Not fatal. The project uses standard Grade Wrapper tasks."
    ((warnings++))
fi
echo ""

# 4. Evaluate Native Compilation Layer (NDK) - Required for JNI
echo -e "${BOLD}[4/5] Checking Android NDK...${NC}"
ndk_located=false
if [ -n "$ANDROID_HOME" ]; then
    if [ -d "$ANDROID_HOME/ndk" ] || [ -d "$ANDROID_HOME/ndk-bundle" ]; then
        ndk_located=true
        echo -e "  ${GREEN}✓ Android NDK directory located under Android SDK.${NC}"
    fi
fi

if [ "$ndk_located" = false ]; then
    echo -e "  ${YELLOW}⚠ Native Core compiler (Android NDK) directory not found on standard paths.${NC}"
    echo "    -> Note: Without NDK, JNI libraries can still rebuild, but rebuilding C/Rust"
    echo "             JNI headers from source will require standard NDK bundle configurations."
    ((warnings++))
fi
echo ""

# 5. Review Project Configuration File Verification
echo -e "${BOLD}[5/5] Checking ZenohBusClient codebase structure...${NC}"
if [ -f "./app/build.gradle.kts" ] && [ -f "./AndroidManifest.xml" ] || [ -f "./app/src/main/AndroidManifest.xml" ]; then
    echo -e "  ${GREEN}✓ Legal Android project architecture found.${NC}"
else
    echo -e "  ${RED}✗ Project files missed. Execute the setup script from project root.${NC}"
    ((errors++))
fi
echo ""

# Summary Evaluation Report
echo -e "${BOLD}${CYAN}======================================================================${NC}"
echo -e "${BOLD}                       EVALUATION REPORT                             ${NC}"
echo -e "${BOLD}${CYAN}======================================================================${NC}"
if [ $errors -eq 0 ]; then
    echo -e "  ${BOLD}${GREEN}CONGRATULATIONS! System meets requirements for building ZenohBusClient!${NC}"
    if [ $warnings -gt 0 ]; then
        echo -e "  ${YELLOW}There are $warnings passive suggestions. Check recommendations above.${NC}"
    fi
    echo ""
    echo "  To compile and generate your APK direct to build path:"
    echo -e "  ${BOLD}gradle assembleDebug${NC}"
else
    echo -e "  ${BOLD}${RED}BUILD ABORTED: $errors critical configuration errors found.${NC}"
    echo "  Please resolve terminal instructions before executing gradle tasks as compilation will fail."
fi
echo -e "${CYAN}======================================================================${NC}"
