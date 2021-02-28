package org.geotools.dbffile;
/** 
	* Constants for DbfFile
	*/
interface DbfConsts{
    int DBF_CENTURY=1900; //base century
    int DBF_MAXLEN=4096;
    int DBF_NAMELEN=11;
    int DBF3_MAXFIELDS=128;
    int DBF4_MAXFIELDS=255;
    int DBF_MAXFIELDS=255;
    int DBF_BUFFSIZE=32;
    int DBF_END_OF_DEFS=13;
    int DBF_OK=1;
    int DBF_READ_HEAD=-101;
    int DBF_BAD_DBFID=-102;
    int DBF_WRITE_HEAD=-103;
    int DBF_READ_DEFS=-111;
    int DBF_TOO_MANY_FIELDS=-112;
    int DBF_NO_FIELDS=-113;
    int DBF_BAD_EODEFS=-114;
    int DBF_WRITE_DEFS=-115;
    int DBF_BAD_ITYPE=-116;
    int DBF_CANNOT_DO_MEMO=-117;
    int DBF_BAD_INT_WIDTH=-118;
    int DBF_BAD_OFFSET=-119;
    int DBF_FLOATING_N=-120;
    int DBF_READ_DATA=-121;
    int DBF_UNPRINT_DATA=-122;
    int DBF_WRITE_DATA=-125;
    int DBF_INT_EXP=-126;
    int DBF_INT_REAL=-127;
    int DBF_INT_JUNK=-128;
    int DBF_REC_DELETED=-129;
    int DBF_ALL_DELETED=-130;
    int DBF_BAD_SIZEOF=-131;
    int DBF_REC_TOO_LONG=-132;
    int DBF_TOO_WIDE_FOR_INF=-133;
    int DBF_MALLOC_FIELD=-134;
    int DBF_MALLOC=-135;
    int DBF_GET_DATE=-150;
}
