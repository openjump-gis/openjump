package org.geotools.dbffile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import org.geotools.misc.FormatedString;

import com.vividsolutions.jump.io.EndianDataOutputStream;

import java.nio.charset.Charset;


/**
 * a class for writing dbf files
 *
 * @author Ian Turton
 * modified by Micha&euml;l MICHAUD on 3 nov. 2004
 */
public class DbfFileWriter implements DbfConsts {

  private final static boolean DEBUG = false;

  private final static String DBC = "DbFW>";

  int NoFields = 1;

  int NoRecs = 0;

  int recLength = 0;

  DbfFieldDef[] fields;

  EndianDataOutputStream ls;

  private boolean header = false;

  private Charset charset = Charset.defaultCharset();

  public DbfFileWriter(String file) throws IOException {
    if (DEBUG)
      System.out.println("---->uk.ac.leeds.ccg.dbffile.DbfFileWriter constructed. Will identify itself as " + DBC);
    ls = new EndianDataOutputStream(new BufferedOutputStream(Files.newOutputStream(Paths.get(file))));
  }

  public void writeHeader(DbfFieldDef[] f, int nrecs) throws IOException {

    NoFields = f.length;
    NoRecs = nrecs;
    fields = new DbfFieldDef[NoFields];
    System.arraycopy(f, 0, fields, 0, NoFields);
    ls.writeByteLE(3); // ID - dbase III with out memo

    // sort out the date
    Calendar calendar = new GregorianCalendar();
    Date trialTime = new Date();
    calendar.setTime(trialTime);
    ls.writeByteLE(calendar.get(Calendar.YEAR) - DBF_CENTURY);
    ls.writeByteLE(calendar.get(Calendar.MONTH) + 1); // month is 0-indexed
    ls.writeByteLE(calendar.get(Calendar.DAY_OF_MONTH));

    int dataOffset = 32 * NoFields + 32 + 1;
    for (int i = 0; i < NoFields; i++) {
      recLength += fields[i].fieldlen;
    }

    recLength++; // delete flag
    if (DEBUG)
      System.out.println(DBC + "rec length " + recLength);
    ls.writeIntLE(NoRecs);
    ls.writeShortLE(dataOffset); // length of header
    ls.writeShortLE(recLength);

    for (int i = 0; i < 20; i++) ls.writeByteLE(0); // 20 bytes of junk!

    // field descriptions
    for (int i = 0; i < NoFields; i++) {
      //patch from Hisaji Ono for Double byte characters
      ls.write(fields[i].fieldname.toString().getBytes(charset.name()), 0, 11); // [Matthias Scholz 04.Sept.2010] Charset added
      ls.writeByteLE(fields[i].fieldtype);
      for (int j = 0; j < 4; j++) ls.writeByteLE(0); // junk
      ls.writeByteLE(fields[i].fieldlen);
      ls.writeByteLE(fields[i].fieldnumdec);
      for (int j = 0; j < 14; j++) ls.writeByteLE(0); // more junk
    }
    ls.writeByteLE(0xd);
    header = true;
  }

  public void writeRecords(Vector<Object>[] recs) throws DbfFileException, IOException {
    if (!header) {
      throw (new DbfFileException("Must write header before records"));
    }
    int i = 0;
    try {
      if (DEBUG) System.out.println(DBC + ":writeRecords writing " + recs.length + " records");
      for (i = 0; i < recs.length; i++) {
        if (recs[i].size() != NoFields) {
          throw new DbfFileException("wrong number of records in " + i + "th record " +
              recs[i].size() + " expected " + NoFields);
        }
        writeRecord(recs[i]);
      }
    } catch (DbfFileException e) {
      throw new DbfFileException(DBC + "at rec " + i + "\n" + e);
    }
  }


  public void writeRecord(Vector<Object> rec) throws DbfFileException, IOException {

    if (!header) {
      throw (new DbfFileException(DBC + "Must write header before records"));
    }

    if (rec.size() != NoFields) {
      throw new DbfFileException(DBC + "wrong number of fields " + rec.size() + " expected " + NoFields);
    }

    ls.writeByteLE(' ');

    for (int i = 0; i < NoFields; i++) {
      int len = fields[i].fieldlen;
      int numdec = fields[i].fieldnumdec;
      Object o = rec.elementAt(i);
      switch (fields[i].fieldtype) {
        case 'C':
        case 'c':
        case 'D': //Added by [Jon Aquino]
        //case 'L': moved to the end by mmichaud
        case 'M':
        case 'G':
          //chars
          if (o == null) {
            // for character type, we cannot differenciate null and empty string in dbf
            o = "";
          }
          StringBuilder tmps = new StringBuilder((String) o);
          while (tmps.toString().getBytes(charset.name()).length < len) {
            //need to fill it with ' ' chars
            tmps.append("                ");
          }
          tmps.setLength(len);
          //patch from Hisaji Ono for Double byte characters
          ls.write(tmps.toString().getBytes(charset.name()), fields[i].fieldstart, len);  // [Matthias Scholz 04.Sept.2010] Charset added
          break;
        case 'N':
        case 'n':
          if (o == null) {
            byte space = ' ';
            byte[] byteArray = new byte[len];
            Arrays.fill(byteArray, space);
            ls.write(byteArray, 0, len);
          } else if (numdec == 0) {
            String fs = "";
            if (o instanceof Integer) {
              fs = FormatedString.format((Integer) o, len);
            }
            // case LONG added by mmichaud on 18 sept. 2004
            else if (o instanceof Long) {
              fs = FormatedString.format(((Long) o).toString(), 0, len);
            } else if (o instanceof java.math.BigDecimal) {
              fs = FormatedString.format(o.toString(), 0, len);
            }
            if (fs.length() > fields[i].fieldlen) {
              fs = FormatedString.format(0, len);
            }
            ls.writeBytesLE(fs);
          } else {
            String fs = "";
            if (o instanceof Double) {
              fs = FormatedString.format(o.toString(), numdec, len);
            } else if (o instanceof java.math.BigDecimal) {
              fs = FormatedString.format(o.toString(), numdec, len);
            }
            if (fs.length() > fields[i].fieldlen) {
              fs = FormatedString.format("0.0", numdec, len);
            }
            ls.writeBytesLE(fs);
          }
          break;
        case 'F':
        case 'f':
          if (o == null) {
            byte space = ' ';
            byte[] byteArray = new byte[len];
            Arrays.fill(byteArray, space);
            ls.write(byteArray, 0, len);
          } else {
            String x = FormatedString.format(o.toString(), numdec, len);
            ls.writeBytesLE(x);
          }
          break;
        // Case 'logical' added by mmichaud on 18 sept. 2004
        case 'L':
          //boolean
          if (o == null || o.equals("") || o.equals(" ") || o.equals("?")) {
            ls.writeBytesLE(" ");
          } else {
            boolean b = (Boolean) o;
            ls.writeBytesLE(b ? "T" : "F");
          }
          break;
      }// switch
    }// fields
  }

  public void close() throws IOException {
    ls.writeByteLE(0x1a); // eof mark
    ls.close();
  }

  //int dp = 2; // default number of decimals to write

  /**
   * @return the charset
   */
  public Charset getCharset() {
    return charset;
  }

  /**
   * @param charset the charset to set
   */
  public void setCharset(Charset charset) {
    this.charset = charset;
  }

}

