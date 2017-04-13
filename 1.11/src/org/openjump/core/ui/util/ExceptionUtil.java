package org.openjump.core.ui.util;

import java.util.*;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.io.datasource.DataSourceQuery;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.HTMLFrame;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;

public class ExceptionUtil {

  public static void reportExceptions(List<Throwable> exceptions,
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

    Collection<Throwable> exceptionsToReport = exceptions.size() <= 10 ?
            exceptions :
            CollectionUtil.concatenate(
                exceptions.subList(0, 5),
                exceptions.subList(exceptions.size() - 5, exceptions.size()));
    for (Throwable exception : exceptionsToReport) {
      workbenchFrame.log(StringUtil.stackTrace(exception));
      outputFrame.append("<li>");
      outputFrame.append(GUIUtil.escapeHTML(
        WorkbenchFrame.toMessage(exception), true, true));
      outputFrame.append("</li>");
      exception.printStackTrace();
    }
    outputFrame.append("</ul>");
  }

  public static void reportExceptions(PlugInContext context, List<Throwable> exceptions) {
    context.getOutputFrame().append("<ul>");
    int size = exceptions.size();
    Collection<Throwable> exceptionsToReport = size <= 10 ?
            exceptions :
            CollectionUtil.concatenate(
                    exceptions.subList(0, 5),
                    exceptions.subList(size - 5, size));
    for (Throwable exception : exceptionsToReport) {
      context.getWorkbenchFrame().log(StringUtil.stackTrace(exception));
      context.getOutputFrame().append("<li>");
      context.getOutputFrame().append(GUIUtil.escapeHTML(
              WorkbenchFrame.toMessage(exception), true, true));
      context.getOutputFrame().append("</li>");
    }
    context.getOutputFrame().append("</ul>");
  }

}
