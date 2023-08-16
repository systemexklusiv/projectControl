package de.systemexklusiv.sysexprojectcontrol.controller;

import com.bitwig.extension.callback.IntegerValueChangedCallback;
import de.systemexklusiv.sysexprojectcontrol.SysexProjectControlExtension;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@ToString
@Getter
@Setter
public class Control implements IntegerValueChangedCallback {
    int index;
    int status;
    int data1;
    int data2;
    boolean isChanged = false;

    // if bitwig changes a value this control gets updated
    @Override
    public void valueChanged(int newValue) {
        SysexProjectControlExtension.p("valueChanged control " + index + " value changed: " + newValue);

        this.isChanged = ( newValue != data2 );

        this.data2 = newValue;
    }
}
