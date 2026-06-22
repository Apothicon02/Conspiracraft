package org.conspiracraft.items;

import org.conspiracraft.Main;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.elements.Element;
import org.conspiracraft.elements.Elements;
import org.conspiracraft.items.types.IceItemType;
import org.conspiracraft.rooms.Cell;
import org.conspiracraft.rooms.Molecule;
import org.conspiracraft.rooms.Room;
import org.conspiracraft.rooms.Rooms;
import org.conspiracraft.world.World;

public class IceItem extends Item implements Cloneable {
    @Override
    public void tick() {
        long time = Main.timeMsLong;
        if (prevTickTime != 0) {
            long dif = time - prevTickTime;
            if (!BlockTypes.blockTypes[World.getBlock(pos.x(), pos.y()-0.125f, pos.z()).x()].blockProperties.isSolid) {
                this.pos.y -= 0.125f;
            }
//            int start = (int)(Math.random()*Math.max(1, World.items.size()-10));
//            int end = Math.min(start+10, World.items.size());
            for (Item randomItem : World.items) {
                //Item randomItem = World.items.get(i);
                if (randomItem.type == type && randomItem.amount < randomItem.type.maxStackSize && Math.abs(randomItem.pos.x() - pos.x()) < 1.f && Math.abs(randomItem.pos.y() - pos.y()) < 1.f && Math.abs(randomItem.pos.z() - pos.z()) < 1.f) {
                    int flow = Math.min(amount, randomItem.type.maxStackSize - randomItem.amount);
                    randomItem.amount += flow;
                    amount -= flow;
                    if (amount <= 0) {
                        World.items.remove(this);
                    }
                    break;
                }
            }
            timeExisted += dif;
            rot += (dif / 50f) * Math.random();
            if (rot >= 360) {
                rot = 0;
            }
            double hoverInc = (dif / 1750f) * Math.min(Math.max(0.01f, 0.1f - hover) * 10, Math.max(0.01f, 0.1f - Math.abs(hover - 0.1f)) * 10) * Math.random();
            if (hoverMeridiem) {
                hover += hoverInc;
                if (hover >= 0.1) {
                    hover = 0.1f;
                    hoverMeridiem = false;
                }
            } else {
                hover -= hoverInc;
                if (hover < 0.f) {
                    hover = 0.f;
                    hoverMeridiem = true;
                }
            }
            int xyz = Rooms.packCellPos((int) pos.x(), (int) pos.y(), (int) pos.z());
            Room room = Rooms.getRoom(xyz);
            if (room != null) {
                Cell cell = room.cells.get(xyz);
                Element element = ((IceItemType) this.type).element;
                double temp = cell.getTemperature();
                if (temp > element.freezingTemp) {
                    int elementId = Elements.elementMap.indexOf(element);
                    Molecule molecule = null;
                    for (Molecule cellMolecule : cell.molecules) {
                        if (cellMolecule.element == elementId) {
                            molecule = cellMolecule;
                            break;
                        }
                    }
                    if (molecule == null) {
                        molecule = new Molecule(elementId, 1);
                    } else {
                        molecule.amount++;
                    }
                    this.amount--;
                    if (this.amount <= 0) {
                        World.items.remove(this);
                    }
                }
            }
        }
        prevTickTime = time;
    }

    @Override
    public IceItem clone() {
        return (IceItem) super.clone();
    }
}
