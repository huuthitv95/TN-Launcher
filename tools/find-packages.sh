#!/bin/sh
# Liệt kê package của app camera 360 / trợ lái trên đầu máy (cần ADB qua mạng hoặc USB).
# Dùng: ./tools/find-packages.sh [serial]
S=${1:+-s $1}
adb $S shell pm list packages -f | grep -i -E "360|avm|camera|winca|adas|lsdt"
