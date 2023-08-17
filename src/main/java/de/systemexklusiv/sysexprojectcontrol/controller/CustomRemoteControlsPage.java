package de.systemexklusiv.sysexprojectcontrol.controller;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.RemoteControl;

import java.util.*;
import java.util.stream.Collectors;

import static de.systemexklusiv.sysexprojectcontrol.SysexProjectControlExtension.p;

public class CustomRemoteControlsPage {

    private static final int PARAMTERS_SIZE = 8;

    // used for the loop while assiging. f.i. a 1 starts mapping remote at index 0 to cc 1
    // The livid code has its first knob at cc num 1 (and not 0 ) that is the motivation for this offset
    public static final int CC_DATA1_OFFSET = 0;

    List<Control> controlsList = new ArrayList<>(32);

    public CursorRemoteControlsPage getRemoteControls() {
        return remoteControls;
    }

    CursorRemoteControlsPage remoteControls;

    public CustomRemoteControlsPage(int startCcNumAt, CursorRemoteControlsPage remoteControls)
    {
        this.remoteControls = remoteControls;

        for (int index = 0; index < PARAMTERS_SIZE; index++) {
            int currentCcNum = startCcNumAt + index;
            this.append(
                    Control.builder()
                            .index(index)
                            .status(176)
                            .data1(currentCcNum)
                            .data2(0)
                            .build()
            );

            // even more than 8 controls have been set up only one bank of 8 react
            this.remoteControls.getParameter(index).value().addValueObserver(128, this.controlAt(index));
        }
    }

    public void append(Control control) {
        this.controlsList.add(control);
    }

    public Control controlAt(int i) {
        return controlsList.get(i);
    }

    public List<Control> changed() {
        return controlsList.stream().filter(control -> control.isChanged()).collect(Collectors.toList());
    }
    public List<Control> all() {
        return controlsList;
    }

    private Optional<Control> getControlByMessage(ShortMidiMessage msg) {
        return this.all().stream()
                .filter(c -> c.getData1() == msg.getData1()).findFirst();
    }
        public void updateRemoteControls(ShortMidiMessage msg) {

            Optional<Control> control = this.getControlByMessage(msg);

            if (control.isPresent()) {

                RemoteControl currentRemote;

                currentRemote = getCurrentRemote(control.get());

                if (Objects.isNull(currentRemote)) {
                    p("no remote control found for index: " + control.get().getIndex());
                    return;
                }

                p("Set remote param: " + currentRemote);

                currentRemote.value().set(msg.getData2(), 128);

            } else p("Control not present!");
        }

        private RemoteControl getCurrentRemote(Control control) {



            RemoteControl currentRemote;

            try {

                currentRemote = this.remoteControls.getParameter(control.getIndex());

            } catch (NoSuchElementException e) {

                p("no remote control found for index: " + control.getIndex());

                return null;
            }

            return currentRemote;
        }

}
