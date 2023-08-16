package de.systemexklusiv.sysexprojectcontrol.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Controller {

    List<Control> controlsList = new ArrayList<>(32);

    public void append(Control control) {
        this.controlsList.add(control);
    }

    public Control at(int i) {
        return controlsList.get(i);
    }

    public List<Control> changed() {
        return controlsList.stream().filter(control -> control.isChanged()).collect(Collectors.toList());
    }
    public List<Control> all() {
        return controlsList;
    }


}
