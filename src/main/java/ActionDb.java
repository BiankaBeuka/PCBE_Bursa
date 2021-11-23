import com.fasterxml.jackson.databind.ObjectMapper;
import org.w3c.dom.html.HTMLSelectElement;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    public Actiune getActionFromList(UUID idActiune){
        for(Actiune a:actionList)
            if(a.getIdActiune()==idActiune)
                return a;
        return null;
    }

    @Override
    public String toString() {
        return "ActionDb{}";
    }
}
