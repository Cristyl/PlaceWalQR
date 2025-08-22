# THE FOLLOWING CODE IS USED FOR GENERATING QR CODES
# USABLE FOR DETECTING PLACES THROUGH THE ANDROID APPLICATION
# WE ARE DEVELOPING, THERE IS NO WARRANTY FOR ITS USE!

import pyqrcode
import png
from pyqrcode import QRCode

place = str(input("insert id of place: "))
print(f"Place converted: {place}")
url = pyqrcode.create(place)
url.png(f"{place}_qr.png", scale=6)