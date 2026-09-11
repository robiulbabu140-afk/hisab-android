package com.hisab.app.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun accountTypeToString(v: AccountType): String = v.name
    @TypeConverter
    fun stringToAccountType(v: String): AccountType = AccountType.valueOf(v)

    @TypeConverter
    fun txnTypeToString(v: TxnType): String = v.name
    @TypeConverter
    fun stringToTxnType(v: String): TxnType = TxnType.valueOf(v)

    @TypeConverter
    fun categoryKindToString(v: CategoryKind): String = v.name
    @TypeConverter
    fun stringToCategoryKind(v: String): CategoryKind = CategoryKind.valueOf(v)

    @TypeConverter
    fun rawSmsStatusToString(v: RawSmsStatus): String = v.name
    @TypeConverter
    fun stringToRawSmsStatus(v: String): RawSmsStatus = RawSmsStatus.valueOf(v)

    @TypeConverter
    fun smsProviderToString(v: SmsProvider): String = v.name
    @TypeConverter
    fun stringToSmsProvider(v: String): SmsProvider = SmsProvider.valueOf(v)

    @TypeConverter
    fun txnSourceToString(v: TxnSource): String = v.name
    @TypeConverter
    fun stringToTxnSource(v: String): TxnSource = TxnSource.valueOf(v)
}
