package io.manycore.debug;

import org.jf.dexlib2.ReferenceType;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.*;
import org.jf.dexlib2.iface.instruction.formats.PackedSwitchPayload;
import org.jf.dexlib2.iface.instruction.formats.SparseSwitchPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PrettyPrint {

    private static String escape(String s) {  // TODO " \n \t \r
        return s;
    }

    static public String instructions(Iterable<? extends Instruction> instructions) {
        StringBuilder sb = new StringBuilder();
        for (Instruction inst : instructions) {
            sb.append(prettyPrintInstruction(inst));
            sb.append("\n");
        }
        return sb.toString();
    }

    static public void compareMethodImplementation(MethodImplementation initial, MethodImplementation updated) {
        System.out.println("Initial Method:");
        System.out.println(PrettyPrint.instructions(initial.getInstructions()));
        System.out.println("-----------------------");
        System.out.println("Condensed Method:");
        System.out.println(PrettyPrint.instructions(updated.getInstructions()));
    }

    static public String prettyPrintInstruction(Instruction instr) {
        StringBuilder sb = new StringBuilder();
        sb.append(instr.getOpcode().toString());

        int A = 0, B = 0, C = 0, D = 0, E = 0, F = 0, G = 0;

        if (instr instanceof FiveRegisterInstruction) {
            FiveRegisterInstruction fiver = (FiveRegisterInstruction)instr;
            if (5 == fiver.getRegisterCount()) {
                sb.append(" v" + fiver.getRegisterC() + ", v" + fiver.getRegisterD() + ", v" + fiver.getRegisterE() + ", v" + fiver.getRegisterF() + ", v" + fiver.getRegisterG());
            } else if (4 == fiver.getRegisterCount()) {
                sb.append(" v" + fiver.getRegisterC() + ", v" + fiver.getRegisterD() + ", v" + fiver.getRegisterE() + ", v" + fiver.getRegisterF());
            } else if (3 == fiver.getRegisterCount()) {
                sb.append(" v" + fiver.getRegisterC() + ", v" + fiver.getRegisterD() + ", v" + fiver.getRegisterE());
            } else if (2 == fiver.getRegisterCount()) {
                sb.append(" v" + fiver.getRegisterC() + ", v" + fiver.getRegisterD());
            } else if (1 == fiver.getRegisterCount()) {
                sb.append(" v" + fiver.getRegisterC());
            }
        } else if (instr instanceof ThreeRegisterInstruction) {
            ThreeRegisterInstruction reg = (ThreeRegisterInstruction) instr;
            A = reg.getRegisterA();
            B = reg.getRegisterB();
            C = reg.getRegisterC();
            sb.append(" v" + A + ", v" + B + ", v" + C);
        } else if (instr instanceof TwoRegisterInstruction) {
            TwoRegisterInstruction reg = (TwoRegisterInstruction) instr;
            A = reg.getRegisterA();
            B = reg.getRegisterB();
            sb.append(" v" + A + ", v" + B);
        } else if (instr instanceof OneRegisterInstruction) {
            OneRegisterInstruction reg = (OneRegisterInstruction) instr;
            A = reg.getRegisterA();
            sb.append(" v" + A);
        } else if (instr instanceof RegisterRangeInstruction) {
            RegisterRangeInstruction rri = (RegisterRangeInstruction) instr;
            A = rri.getStartRegister();
            sb.append(" {v" + A + " .. v" + (rri.getStartRegister() + rri.getRegisterCount()) + "}");    // ????
        }

        if (instr instanceof DualReferenceInstruction) {
            DualReferenceInstruction duref = (DualReferenceInstruction) instr;
            if (duref.getReferenceType() == ReferenceType.STRING) {
                sb.append(", \"" + duref.getReference().toString() + "\"");
            } else {
                sb.append(", " + duref.getReference().toString());
            }

            if (duref.getReferenceType2() == ReferenceType.STRING) {
                sb.append(", \"" + escape(duref.getReference2().toString()) + "\"");
            } else {
                sb.append(", " + duref.getReference2().toString());
            }
        } else if (instr instanceof ReferenceInstruction) {
            ReferenceInstruction ref = (ReferenceInstruction) instr;
            if (ref.getReferenceType() == ReferenceType.STRING) {
                sb.append(", \"" + ref.getReference().toString() + "\"");
            } else {
                sb.append(", " + ref.getReference().toString());
            }
        }

        if (instr instanceof OffsetInstruction) {
            OffsetInstruction oi = (OffsetInstruction) instr;
            if (oi.getCodeOffset() >= 0) {
                sb.append(String.format(" [-> +%d]", oi.getCodeOffset()));
            } else {
                sb.append(String.format(" [-> %d]", oi.getCodeOffset()));
            }
        }

        if (instr instanceof WideLiteralInstruction) {
            WideLiteralInstruction wli = (WideLiteralInstruction) instr;
            sb.append(String.format(", %d", wli.getWideLiteral()));
        } else if (instr instanceof NarrowLiteralInstruction) {
            NarrowLiteralInstruction nli = (NarrowLiteralInstruction) instr;
            sb.append(String.format(", %d", nli.getNarrowLiteral()));
        }

        if (instr instanceof PackedSwitchPayload) {
            PackedSwitchPayload psp = (PackedSwitchPayload) instr;
            List<String> labelList = new ArrayList<>();
            for (SwitchElement se : psp.getSwitchElements()) {
                labelList.add("[key:" + se.getKey() + ",offset:" + se.getOffset() + "]");
            }
            sb.append(" ");
            sb.append(String.join(", ", labelList));
        } else if (instr instanceof SparseSwitchPayload) {
            SparseSwitchPayload ssp = (SparseSwitchPayload) instr;
            List<String> labelList = new ArrayList<>();
            for (SwitchElement se : ssp.getSwitchElements()) {
                labelList.add("[key:" + se.getKey() + ",offset:" + se.getOffset() + "]");
            }
            sb.append(" ");
            sb.append(String.join(", ", labelList));
        }

        return sb.toString();
    }

    static public String prettyPrintField(Field field) {
        return field.toString() + " " + field.getAccessFlags();
    }

    static public List<String> prettyPrintMethod(Method method) {
        List<String> o = new ArrayList<>();
        o.add(method.toString());
        /*
        DalvikMethodSignature ms = new DalvikMethodSignature(method);
        o.add(method.toString());
        o.add(String.format("  %-32s          .locals %d # excludes parameters", "", ms.getFirstParamIndex()));
         */

        MethodImplementation mi = method.getImplementation();
        if (null != mi) {
            int nbLine = 0;
            int codeOffs = 0;
            for (Instruction inst : mi.getInstructions()) {
                String[] instStrArr = inst.toString().split("\\.");
                String s = String.format("  %-32s %3d %4d %s", instStrArr[instStrArr.length - 1], nbLine, codeOffs, PrettyPrint.prettyPrintInstruction(inst));
                o.add(s);
                nbLine++;
                codeOffs += inst.getCodeUnits();
            }
        }

        return o;
    }

    static public List<String> prettyPrintClassDef(ClassDef cd) {
        List<String> o = new ArrayList<>();
        for (Field field : cd.getFields()) {
            o.add(prettyPrintField(field));
        }

        for (Method method : cd.getMethods()) {
            o.addAll(prettyPrintMethod(method));
        }

        return o;
    }


    static public List<Method> getMethods(Iterable<? extends ClassDef> classDefsIterator, String contains) {
        List<Method> out = new ArrayList<>();
        for (ClassDef cd : classDefsIterator) {
            for (Method m : cd.getMethods()) {
                if (m.toString().contains(contains)) {
                    out.add(m);
                }
            }
        }
        return out;
    }


    static public String methodToAssemblyInstruction(Instruction instr, int nbLine, int codeOffs, Map<Integer, Integer> switchDataToCallerOffs) {
        StringBuilder sb = new StringBuilder();
        sb.append("asm." + instr.getOpcode().toString() + "(");

        List<String> argList = new ArrayList<>();

        int A = 0, B = 0, C = 0, D = 0, E = 0, F = 0, G = 0;
        if (instr instanceof FiveRegisterInstruction) {
            FiveRegisterInstruction fri = (FiveRegisterInstruction) instr;
            argList.add("" + fri.getRegisterCount());
            argList.add("" + fri.getRegisterC());
            argList.add("" + fri.getRegisterD());
            argList.add("" + fri.getRegisterE());
            argList.add("" + fri.getRegisterF());
            argList.add("" + fri.getRegisterG());
        } else if (instr instanceof ThreeRegisterInstruction) {
            ThreeRegisterInstruction reg = (ThreeRegisterInstruction) instr;
            argList.add("" + reg.getRegisterA());
            argList.add("" + reg.getRegisterB());
            argList.add("" + reg.getRegisterC());
        } else if (instr instanceof TwoRegisterInstruction) {
            TwoRegisterInstruction reg = (TwoRegisterInstruction) instr;
            argList.add("" + reg.getRegisterA());
            argList.add("" + reg.getRegisterB());
        } else if (instr instanceof OneRegisterInstruction) {
            OneRegisterInstruction reg = (OneRegisterInstruction) instr;
            argList.add("" + reg.getRegisterA());
        } else if (instr instanceof RegisterRangeInstruction) {
            RegisterRangeInstruction rri = (RegisterRangeInstruction) instr;
            argList.add("" + rri.getStartRegister());
            argList.add("" + rri.getRegisterCount());
        }

        if (instr instanceof DualReferenceInstruction) {
            DualReferenceInstruction duref = (DualReferenceInstruction) instr;
            argList.add("\"" + duref.getReference().toString() + "\"");
            argList.add("\"" + duref.getReference2().toString() + "\"");
        } else if (instr instanceof ReferenceInstruction) {
            ReferenceInstruction ref = (ReferenceInstruction) instr;
            argList.add("\"" + escape(ref.getReference().toString()) + "\"");
        } else if (instr instanceof NarrowLiteralInstruction) {
            NarrowLiteralInstruction nli = (NarrowLiteralInstruction) instr;
            argList.add("" + nli.getNarrowLiteral());
        } else if (instr instanceof WideLiteralInstruction) {
            WideLiteralInstruction wli = (WideLiteralInstruction) instr;
            argList.add("" + wli.getWideLiteral() + "L");
        } else if (instr instanceof OffsetInstruction) {
            OffsetInstruction oi = (OffsetInstruction) instr;
            argList.add("\"L" + (codeOffs + oi.getCodeOffset()) + "\"");
        }

        if (instr instanceof PackedSwitchPayload) {
            assert (switchDataToCallerOffs.containsKey(codeOffs));
            int labelAdd = switchDataToCallerOffs.get(codeOffs).intValue();

            PackedSwitchPayload psp = (PackedSwitchPayload) instr;
            if (psp.getSwitchElements().size() == 0) {
                argList.add("0");
                argList.add("null");
            } else {
                int startKey = psp.getSwitchElements().get(0).getKey();
                List<String> labelList = new ArrayList<>();
                for (SwitchElement se : psp.getSwitchElements()) {
                    labelList.add("\"L" + (se.getOffset() + labelAdd) + "\"");
                }
                // Arrays.asList("asd", "asd")
                argList.add("0");
                argList.add("new ArrayList<String>(Arrays.asList(" + String.join(", ", labelList) + "))");
            }
        } else if (instr instanceof SparseSwitchPayload) {
            assert (switchDataToCallerOffs.containsKey(codeOffs));
            int labelAdd = switchDataToCallerOffs.get(codeOffs).intValue();

            SparseSwitchPayload ssp = (SparseSwitchPayload) instr;
            List<String> keys = new ArrayList<>();
            List<String> labelList = new ArrayList<>();
            for (SwitchElement se : ssp.getSwitchElements()) {
                keys.add("" + se.getKey());
                labelList.add("\"L" + (se.getOffset() + labelAdd) + "\"");
            }
            argList.add("new ArrayList<Integer>(Arrays.asList(" + String.join(", ", keys) + "))");
            argList.add("new ArrayList<String>(Arrays.asList(" + String.join(", ", labelList) + "))");
        }

        sb.append(String.join(", ", argList));
        sb.append(");");

        return sb.toString();
    }


}
