package org.openjump.core.ui.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.io.datasource.DataSourceQuery;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.HTMLFrame;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;

public class ExceptionUtil {

  public static void reportExceptions(ArrayList exceptions,
    DataSourceQuery dataSourceQuery, WorkbenchFrame workbenchFrame,
    HTMLFrame outputFrame) {
    outputFrame.addHeader(
      1,
      exceptions.size()
        + " "
        + I18N.get("datasource.LoadDatasetPlugIn.problem")
        + StringUtil.s(exceptions.size())
        + " "
        + I18N.get("datasource.LoadDatasetPlugIn.loading")
        + " "
        + dataSourceQuery.toString()
        + "."
        + ((exceptions.size() > 10) ? I18N.get("datasource.LoadDatasetPlugIn.first-and-last-five")
          : ""));
    outputFrame.addText(I18N.get("datasource.LoadDatasetPlugIn.see-view-log"));
    outputFrame.append("<ul>");

    Collection exceptionsToReport = exceptions.size() <= 10 ? exceptions
      : CollectionUtil.concatenate(Arrays.asList(new Collection[] {
        exceptions.subList(0, 5),
        exceptions.subList(exceptions.size() - 5, exceptions.size())
      }));
    for (Iterator j = exceptionsToReport.iterator(); j.hasNext();) {
      Exception exception = (Exception)j.next();
      workbenchFrame.log(StringUtil.stackTrace(exception));
      outputFrame.append("<li>");
      outputFrame.append(GUIUtil.escapeHTML(
        WorkbenchFrame.toMessage(exception), true, true));
      outputFrame.append("</li>");
      exception.printStackTrace();
    }
    outputFrame.append("</ul>");
  }

}
