package de.systemexklusiv.sysexprojectcontrol;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.*;
import de.systemexklusiv.sysexprojectcontrol.controller.CustomRemoteControlsPage;

import java.util.ArrayList;
import java.util.List;

public class SysexProjectControlExtension extends ControllerExtension {

    public static final int PARAMTERS_SIZE = 8;

    public static ControllerHost host;
    private static final boolean DEBUG = true;

    MidiOut midiOut;
    MidiIn midiIn;

    protected SysexProjectControlExtension(final SysexProjectControlExtensionDefinition definition, final ControllerHost host) {
        super(definition, host);
    }

    List<CustomRemoteControlsPage> customRemoteControlsPages = new ArrayList<>();

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

        CursorRemoteControlsPage remoteControls1 = rootTrackGroup
                .createCursorRemoteControlsPage("1", PARAMTERS_SIZE, "1");
        CursorRemoteControlsPage remoteControls2 = rootTrackGroup
                .createCursorRemoteControlsPage("2", PARAMTERS_SIZE, "2");
        CursorRemoteControlsPage remoteControls3 = rootTrackGroup
                .createCursorRemoteControlsPage("3", PARAMTERS_SIZE, "3");
        CursorRemoteControlsPage remoteControls4 = rootTrackGroup
                .createCursorRemoteControlsPage("4", PARAMTERS_SIZE, "4");

        customRemoteControlsPages.add(new CustomRemoteControlsPage(1, remoteControls1));
        customRemoteControlsPages.add(new CustomRemoteControlsPage(9, remoteControls2));
        customRemoteControlsPages.add(new CustomRemoteControlsPage(17,remoteControls3));
        customRemoteControlsPages.add(new CustomRemoteControlsPage(25,remoteControls4));


        p("SysexProjectControl Initialized");
    }

    @Override
    public void flush() {
        customRemoteControlsPages.stream().forEach(page -> page.changed().forEach(
                c -> {
                    p("flush: " + c);
                    midiOut.sendMidi(176, c.getData1(), c.getData2());
                    c.setChanged(false);
                }
        ));

    }

    /**
     * Called when we receive midi from controller
     */
    private void onMidi0(ShortMidiMessage msg) {
        p(msg.toString());

        customRemoteControlsPages.stream().forEach(page -> page.updateRemoteControls(msg));
    }

    public static void p(String text) {
        if (DEBUG)
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
        switch (data) {
            case "f07f7f0605f7":
                mTransport.rewind();
                break;
            case "f07f7f0604f7":
                mTransport.fastForward();
                break;
            case "f07f7f0601f7":
                mTransport.stop();
                break;
            case "f07f7f0602f7":
                mTransport.play();
                break;
            case "f07f7f0606f7":
                mTransport.record();
                break;
        }
    }

    private Transport mTransport;
}
