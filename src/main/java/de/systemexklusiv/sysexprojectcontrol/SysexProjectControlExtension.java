package de.systemexklusiv.sysexprojectcontrol;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extension.controller.ControllerExtension;
import de.systemexklusiv.sysexprojectcontrol.controller.Control;
import de.systemexklusiv.sysexprojectcontrol.controller.Controller;

import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Maps a controller with CC -> status 176; data 1 1-8 to the Project Remote Controls
 *
 * I am pondering on how to get more than just the 8 contrls per Bank. In my case I have
 * a livid Instrumets Code and I like to have all 32 knobs at one controlling something
 */
public class SysexProjectControlExtension extends ControllerExtension {

    public static final int PARAMTERS_SIZE = 32;

    // used for the loop while assiging. f.i. a 1 starts mapping remote at index 0 to cc 1
    // The livid code has its first knob at cc num 1 (and not 0 ) that is the motivation for this offset
    public static final int CC_DATA1_OFFSET = 1;

    public static ControllerHost host;

    Controller controller;
    MidiOut midiOut;
    MidiIn midiIn;

    CursorRemoteControlsPage remoteControls1;

    protected SysexProjectControlExtension(final SysexProjectControlExtensionDefinition definition, final ControllerHost host) {
        super(definition, host);
    }

    @Override
    public void init() {

        host = getHost();

        mTransport = host.createTransport();

        midiIn = host.getMidiInPort(0);

        host.getMidiInPort(0).setMidiCallback((ShortMidiMessageReceivedCallback) msg -> onMidi0(msg));
        host.getMidiInPort(0).setSysexCallback((String data) -> onSysex0(data));
        midiOut = host.getMidiOutPort(0);

        Project project = host.getProject();
        Track rootTrackGroup = project.getRootTrackGroup();

        remoteControls1 = rootTrackGroup.createCursorRemoteControlsPage(PARAMTERS_SIZE);

        // Throws error, there con only be one main remote
        //CursorRemoteControlsPage remoteControls2 = rootTrackGroup.createCursorRemoteControlsPage(PARAMTERS_SIZE);


        controller = new Controller();

        for (int index = 0; index < PARAMTERS_SIZE; index++) {
            controller.append(
                    Control.builder()
                            .index(index)
                            .status(176)
                            .data1(index + CC_DATA1_OFFSET)
                            .data2(0)
                            .build()
            );

            // even more than 8 controls have been set up only one bank of 8 react
            remoteControls1.getParameter(index).value().addValueObserver(128, controller.at(index));
        }

        p("SysexProjectControl Initialized");
    }

    @Override
    public void flush() {

        controller.changed().forEach(
                c -> {
                    p("flush: " + c);
                    midiOut.sendMidi(176, c.getData1(), c.getData2());
                    c.setChanged(false);
                }
        );

    }

    /**
     * Called when we receive midi from controller
     */
    private void onMidi0(ShortMidiMessage msg) {
        p(msg.toString());

        Optional<Control> control = controller.all().stream()
                .filter(c -> c.getData1() == msg.getData1()).findFirst();

        if (control.isPresent()) {

            RemoteControl currentRemote = null;

            try {

                currentRemote = remoteControls1.getParameter(control.get().getIndex());

            } catch (NoSuchElementException e) {

                p("no remote control found for index: " + control.get().getIndex());

                return;
            }

            p("Set remote param: " + currentRemote.toString());

            currentRemote.value().set(msg.getData2(), 128);

        } else p("Control not present!");

    }


    public static void p(String text) {
        host.println(text);
    }

    public static void p(int text) {
        host.println(String.valueOf(text));
    }

    @Override
    public void exit() {
        p("SysexProjectControl Exited");
    }


    /**
     * Called when we receive sysex MIDI message on port 0.
     */
    private void onSysex0(final String data) {
        // MMC Transport Controls:
        if (data.equals("f07f7f0605f7"))
            mTransport.rewind();
        else if (data.equals("f07f7f0604f7"))
            mTransport.fastForward();
        else if (data.equals("f07f7f0601f7"))
            mTransport.stop();
        else if (data.equals("f07f7f0602f7"))
            mTransport.play();
        else if (data.equals("f07f7f0606f7"))
            mTransport.record();
    }

    private Transport mTransport;
}
