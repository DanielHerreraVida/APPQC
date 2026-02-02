package com.example.qceqapp.data.network

object Constants {
 const val DEV_URL = "https://dev2.vida18.com:214/"
 const val STAGE_URL = "https://stagevf4.vida18.com:215/"
 const val PROD_URL = "https://wsapi.vida18.com:8089/"

 var BASE_URL: String = PROD_URL
 const val VERSION = "V2.1.4"
 const val API_USER = "95C59F52-8DB0-4D81-A7A1-43EF0CF216EB"
 const val API_PASSWORD = "55B0ECDC-7071-4BC2-89CA-7797AC67B3A9997727DD-F84E-4D48-B70C-97D710C8DCAF"
 var token: String = ""
 const val CONNECT_TIMEOUT = 30L
 const val READ_TIMEOUT = 30L
 const val WRITE_TIMEOUT = 30L
}