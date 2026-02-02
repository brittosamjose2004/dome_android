#!/bin/bash

# Android SDK Setup Script
# This script downloads and sets up the Android SDK Command Line Tools

set -e

echo "🚀 Android SDK Setup"
echo "===================="
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java is not installed!${NC}"
    echo "Please install Java JDK 17 or higher:"
    echo "  Ubuntu/Debian: sudo apt install openjdk-17-jdk"
    echo "  MacOS: brew install openjdk@17"
    exit 1
fi

echo -e "${GREEN}✓ Java found:${NC} $(java -version 2>&1 | head -n 1)"

# Set SDK location
SDK_ROOT="${HOME}/Android/Sdk"
if [ -n "$ANDROID_HOME" ]; then
    SDK_ROOT="$ANDROID_HOME"
fi

echo -e "\n${BLUE}SDK will be installed to:${NC} $SDK_ROOT"

# Create SDK directory
mkdir -p "$SDK_ROOT"
cd "$SDK_ROOT"

# Detect OS
OS="linux"
if [[ "$OSTYPE" == "darwin"* ]]; then
    OS="mac"
fi

echo -e "\n${YELLOW}📥 Downloading Android Command Line Tools...${NC}"

# Download command line tools
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-${OS}-9477386_latest.zip"
CMDLINE_TOOLS_ZIP="commandlinetools.zip"

if [ -f "$CMDLINE_TOOLS_ZIP" ]; then
    echo "Command line tools already downloaded"
else
    wget -q --show-progress "$CMDLINE_TOOLS_URL" -O "$CMDLINE_TOOLS_ZIP" || curl -# -L "$CMDLINE_TOOLS_URL" -o "$CMDLINE_TOOLS_ZIP"
fi

# Extract
echo -e "\n${YELLOW}📦 Extracting...${NC}"
unzip -q -o "$CMDLINE_TOOLS_ZIP"
rm "$CMDLINE_TOOLS_ZIP"

# Move to correct location
mkdir -p cmdline-tools/latest
if [ -d "cmdline-tools/bin" ]; then
    mv cmdline-tools/bin cmdline-tools/latest/
    mv cmdline-tools/lib cmdline-tools/latest/
    [ -f "cmdline-tools/NOTICE.txt" ] && mv cmdline-tools/NOTICE.txt cmdline-tools/latest/
    [ -f "cmdline-tools/source.properties" ] && mv cmdline-tools/source.properties cmdline-tools/latest/
fi

# Set environment variables
export ANDROID_HOME="$SDK_ROOT"
export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools"

echo -e "\n${YELLOW}📦 Installing Android SDK components...${NC}"
echo "This may take a few minutes..."

# Accept licenses
yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses 2>/dev/null || true

# Install required components
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" \
    "platform-tools" \
    "platforms;android-34" \
    "build-tools;34.0.0" \
    "cmdline-tools;latest"

echo -e "\n${GREEN}✅ Android SDK installed successfully!${NC}"
echo ""
echo -e "${YELLOW}📝 Add these to your shell profile (~/.bashrc or ~/.zshrc):${NC}"
echo ""
echo "export ANDROID_HOME=\"$SDK_ROOT\""
echo "export PATH=\$PATH:\$ANDROID_HOME/cmdline-tools/latest/bin"
echo "export PATH=\$PATH:\$ANDROID_HOME/platform-tools"
echo ""

# Add to current session
export ANDROID_HOME="$SDK_ROOT"
export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools"

echo -e "${BLUE}For current session, environment is already set.${NC}"
echo ""
echo -e "${GREEN}🎉 Setup complete! You can now build the Android app.${NC}"
echo ""
echo "Next steps:"
echo "  1. cd android-app"
echo "  2. ./build.sh debug"
echo ""
