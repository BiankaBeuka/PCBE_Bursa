import com.fasterxml.jackson.databind.ObjectMapper;
import org.w3c.dom.html.HTMLSelectElement;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ActionDb {

    static List<Actiune>  actionList = new ArrayList<Actiune>();

    public static void saveAction(Actiune action) {
        actionList.add(action);
    }

    public static List<Actiune> getActionList() {
        return actionList;
    }

    public static void setActionList(List<Actiune> actionList) {
        ActionDb.actionList = actionList;
    }



}
