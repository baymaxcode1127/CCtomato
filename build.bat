@echo off
chcp 65001 >nul
echo ===============================
echo   番茄钟 Pomodoro Timer - 构建
echo ===============================
echo.

:: Clean
if exist out rmdir /s /q out
mkdir out

:: Compile
echo [1/2] 编译源代码...
javac -d out -encoding UTF-8 -cp out src\pomodoro\Main.java src\pomodoro\model\*.java src\pomodoro\util\*.java src\pomodoro\view\*.java src\pomodoro\controller\*.java
if %ERRORLEVEL% NEQ 0 (
    echo 编译失败！
    pause
    exit /b 1
)
echo 编译成功！

:: Package
echo [2/2] 打包 JAR...
jar cfm pomodoro.jar MANIFEST.MF -C out .
if %ERRORLEVEL% NEQ 0 (
    echo 打包失败！
    pause
    exit /b 1
)
echo 打包成功！

echo.
echo ✅ 构建完成！双击 pomodoro.jar 运行番茄钟。
echo    或执行: java -jar pomodoro.jar
pause
