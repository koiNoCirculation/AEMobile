#!/bin/bash
if [[ ! -d "build" ]]; then
  mkdir -p build
fi
arch=`uname -m`
os=`uname`
NPM=`which npm`
if [[ $? -eq 1 ]]; then
  if [[ $os == 'Darwin' ]]; then
    if [[ $arch == 'arm64' ]]; then
      url='https://nodejs.org/dist/v22.21.1/node-v22.21.1-darwin-arm64.tar.gz'
      d='node-v22.21.1-darwin-arm64'
    else
      url='https://nodejs.org/dist/v22.21.1/node-v22.21.1-darwin-x64.tar.gz'
      d='node-v22.21.1-darwin-x64'
    fi
  else
    if [[ $arch == 'x86_64' ]]; then
      url='https://nodejs.org/dist/v22.21.1/node-v22.21.1-linux-x64.tar.xz'
      d='node-v22.21.1-linux-x64'
    else
      url='https://nodejs.org/dist/v22.21.1/node-v22.21.1-linux-arm64.tar.xz'
      d='node-v22.21.1-linux-arm64'
    fi
  fi
  if [[ ! -d build/$d ]]; then
      curl $url -o build/node.tar.xz && cd build && tar -xf node.tar.xz && cd ../
  fi
  NPM="../build/${d}/bin/npm"
fi

git submodule update --init --recursive
if [[ $1 == 'build' ]]; then
    cd AEMonitor-Frontend && $NPM install && $NPM run build
    mkdir -p ../src/main/resources/static && cp -r build/* ../src/main/resources/static
    cd ../
fi
./gradlew.in $@