package org.conspiracraft.rooms;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import org.conspiracraft.items.types.ItemType;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.elements.Element;
import org.conspiracraft.elements.Elements;
import org.conspiracraft.items.IceItem;
import org.conspiracraft.items.Item;
import org.conspiracraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Rooms {
    public static Vector3i min = new Vector3i();
    public static Vector3i max = new Vector3i();
    public static int maxSize = 50;
    public static List<Room> rooms = new ArrayList<>();
    public static Room currentScan = new Room();
    public static Random roomRandom = new Random(911);

    public static void tick() {
        int offset = roomRandom.nextInt(5000);
        ArrayList<Room> roomsToRemove = new ArrayList<>();
        for (Room room : rooms) {
            boolean sealed = true;
            boolean matchesGlobal = true;
            Cell globalCell = World.worldType.getGlobalAtmo();
            for (int xyz : room.cells.keySet()) {
                Cell cell = room.cells.get(xyz);
                Vector3i pos = unpackCellPos(xyz);

                double cellTemp = cell.getTemperature();
                offset--;
                if (offset <= 0) {
                    offset = 5000;
                    for (Molecule molecule : cell.molecules) {
                        if (molecule.amount >= 1) {
                            Element element = Elements.elementMap.get(molecule.element);
                            if (cellTemp < element.freezingTemp) {
                                molecule.amount--;
                                ItemType itemType = element.iceItemType;
                                boolean itemExisted = false;
                                for (Item item : World.items) {
                                    if (item.type == itemType && item.amount < item.type.maxStackSize) {
                                        if (Math.abs(item.pos.x() - pos.x()) < 1.f && Math.abs(item.pos.y() - pos.y()) < 1.f && Math.abs(item.pos.z() - pos.z()) < 1.f) {
                                            item.amount++;
                                            itemExisted = true;
                                            break;
                                        }
                                    }
                                }
                                if (!itemExisted) {
                                    World.items.add(new IceItem().type(itemType).moveTo(new Vector3f(pos.x() + 0.25f, pos.y() + 0.25f, pos.z() + 0.25f)));
                                }
                            }
                        }
                    }
                }

                int randomOffset = roomRandom.nextInt(6);
                Vector3i[] neighbors = new Vector3i[]{
                        (new Vector3i(pos.x() + 1, pos.y(), pos.z())),
                        (new Vector3i(pos.x() - 1, pos.y(), pos.z())),
                        (new Vector3i(pos.x(), pos.y() + 1, pos.z())),
                        (new Vector3i(pos.x(), pos.y() - 1, pos.z())),
                        (new Vector3i(pos.x(), pos.y(), pos.z() + 1)),
                        (new Vector3i(pos.x(), pos.y(), pos.z() - 1))};
                for (int i = randomOffset; i < randomOffset+6; i++) {
                    int idx = i-(((int)(i/6))*6);
                    Vector3i nPos = neighbors[idx];
                    boolean flowAnything = true;
                    boolean flowMoles = true;
                    Cell nCell = room.cells.get(packCellPos(nPos.x(), nPos.y(), nPos.z()));
                    if (nCell == null) {
                        nCell = new Cell(globalCell);
                        if (!BlockTypes.blockTypes[World.getBlockTypeUnchecked(nPos.x(), nPos.y(), nPos.z())].permeable()) {
                            flowMoles = false;
                            flowAnything = false;//disables global temperature exchange through walls when uncommented
                        } else {
                            sealed = false;
                        }
                    }
                    if (flowAnything) {
                        for (Molecule molecule : cell.molecules) {
                            int cellMoles = 0;
                            for (Molecule aMolecule : cell.molecules) {
                                cellMoles += aMolecule.amount;
                            }
                            double massLost = 0.d;
                            if (flowMoles) {
                                Molecule nMolecule = null;
                                for (Molecule potentialNMolecule : nCell.molecules) {
                                    if (molecule.element == potentialNMolecule.element) {
                                        nMolecule = potentialNMolecule;
                                        break;
                                    }
                                }
                                boolean doesMoleculeReallyExist = true;
                                if (nMolecule == null) {
                                    doesMoleculeReallyExist = false;
                                    nMolecule = new Molecule(molecule.element, 0);
                                }
                                double moleFlow = Math.ceilDiv(molecule.amount - nMolecule.amount, 2);
                                if (moleFlow > 0) {
                                    massLost = moleFlow / cellMoles;
                                    molecule.amount -= (int) moleFlow;
                                    nMolecule.amount += (int) moleFlow;
                                    if (!doesMoleculeReallyExist) {
                                        nCell.molecules.add(nMolecule);
                                    }
                                }
                            }
                            cellTemp = cell.getTemperature();
                            double nCellTemp = nCell.getTemperature();
                            double tempFlow = cell.getEnergyFromTemperature((cellTemp - nCellTemp) / 2) * ((double) molecule.amount / cellMoles);
                            if (tempFlow < 0) {
                                tempFlow = 0;
                            }
                            if (cellTemp >= nCellTemp) {
                                tempFlow = Math.max(1, tempFlow);
                            }
                            if (massLost < 0) {
                                massLost = 0;
                            }
                            long energyFlow = (long) (Math.max(cell.energy * massLost, tempFlow));
                            if (energyFlow != 0) {
                                cell.energy -= energyFlow;
                                nCell.energy += energyFlow;
                            }
                        }
                        cell.molecules.removeIf((molecule) -> molecule.amount <= 0);
                        nCell.molecules.removeIf((molecule) -> molecule.amount <= 0);
                    }
                }

                if (matchesGlobal) {
                    if (Math.abs(cell.energy - globalCell.energy) > 100) {
                        matchesGlobal = false;
                    } else if (cell.molecules.size() != globalCell.molecules.size()) {
                        matchesGlobal = false;
                    } else {
                        ByteArrayList elements = new ByteArrayList();
                        breakingPoint:
                        for (Molecule molecule : cell.molecules) {
                            for (Molecule globalMolecule : globalCell.molecules) {
                                elements.addLast(molecule.element);
                                if (globalMolecule.element == molecule.element) {
                                    if (Math.abs(globalMolecule.amount - molecule.amount) > 0) {
                                        matchesGlobal = false;
                                        break breakingPoint;
                                    }
                                }
                            }
                        }
                        if (matchesGlobal) {
                            for (byte element : World.worldType.getGlobalElements()) {
                                if (!elements.contains(element)) {
                                    matchesGlobal = false;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            if (matchesGlobal && !sealed) {
                roomsToRemove.add(room);
            }
        }
        rooms.removeAll(roomsToRemove);
    }
    public static Room generateRoomIfNeeded(Vector3i pos) {
        Room room = getRoom(pos);
        if (room == null) {
            room = detectRoom(pos.x, pos.y, pos.z);
        }
        return room;
    }
    public static void inject(Vector3i pos, Molecule molecule, long energy) {
        Room room = generateRoomIfNeeded(pos);
        if (room != null) {
            int xyz = packCellPos(pos);
            Cell cell = room.cells.get(xyz);
            Molecule exists = null;
            for (Molecule cellMolecule : cell.molecules) {
                if (cellMolecule.element == molecule.element) {
                    exists = cellMolecule;
                    break;
                }
            }
            cell.energy += energy;
            if (cell.energy < 0) {
                cell.energy = Long.MAX_VALUE-100;
            }
            if (exists != null) {
                exists.amount += molecule.amount;
            } else {
                cell.molecules.add(molecule);
            }
        }
    }
    public static void mulEnergy(Vector3i pos, int energyMul) {
        Room room = generateRoomIfNeeded(pos);
        if (room != null) {
            int xyz = packCellPos(pos);
            Cell cell = room.cells.get(xyz);
            cell.energy *= energyMul;
            if (cell.energy < 0) {
                cell.energy = Long.MAX_VALUE-100;
            }
        }
    }
    public static void removeCell(Vector3i pos) {
        Room room = getRoom(pos);
        if (room != null) {
            int xyz = packCellPos(pos);
            room.cells.remove(xyz);
        }
    }

    public static Room getRoom(int ogxyz) {
        for (Room room : rooms) {
            if (room.cells.keySet().contains(ogxyz)) {
                return room;
            }
        }
        return null;
    }
    public static Room getRoom(Vector3i pos) {
        return getRoom(packCellPos(pos.x(), pos.y(), pos.z()));
    }

    public static void detectRooms(int x, int y, int z) {
        detectRoom(x, y, z);
        detectRoom(x+1, y, z);
        detectRoom(x-1, y, z);
        detectRoom(x, y+1, z);
        detectRoom(x, y-1, z);
        detectRoom(x, y, z+1);
        detectRoom(x, y, z-1);
    }
    public static Room detectRoom(int x, int y, int z) {
        min = new Vector3i(x-maxSize, y-maxSize, z-maxSize);
        min.max(new Vector3i(1));
        max = new Vector3i(x+maxSize, y+maxSize, z+maxSize);
        max.min(new Vector3i(World.size-2, World.height-2, World.size-2));
        currentScan = new Room();
        if (scanCells(x, y, z) && !currentScan.cells.isEmpty()) {
            mergeRooms(currentScan);
            rooms.addLast(currentScan);
            currentScan = null;
            return rooms.getLast();
        }
        currentScan = null;
        return null;
    }

    private static void mergeRooms(Room mergedRoom) {
        boolean wasRoomHereAlready = false;
        List<Room> roomsToRemove = new ArrayList<>();
        for (Room room : rooms) {
            for (int ogxyz : room.cells.keySet()) {
                if (mergedRoom.cells.containsKey(ogxyz)) {
                    wasRoomHereAlready = true;
                    for (int xyz : room.cells.keySet()) {
                        Cell cell = room.cells.get(xyz);
                        Cell mergedCell = mergedRoom.cells.get(xyz);
                        if (mergedCell == null) {
                            mergedCell = new Cell();
                            mergedRoom.cells.put(xyz, mergedCell);
                        }
                        mergedCell.energy += cell.energy;
                        for (Molecule molecule : cell.molecules) {
                            boolean matched = false;
                            for (Molecule mergedMolecule : mergedCell.molecules) {
                                if (molecule.element == mergedMolecule.element) {
                                    mergedMolecule.amount += molecule.amount;
                                    matched = true;
                                    break;
                                }
                            }
                            if (!matched) {
                                mergedCell.molecules.add(molecule);
                            }
                        }
                    }
                    roomsToRemove.add(room);
                    break;
                }
            }
        }
        for (Map.Entry<Integer, Cell> entry : mergedRoom.cells.entrySet()) {
            if (entry.getValue() == null) {
                entry.setValue(wasRoomHereAlready ? new Cell() : new Cell(World.worldType.getGlobalAtmo()));
            }
        }
        rooms.removeIf(roomsToRemove::contains);
    }

    public static boolean scanCells(int x, int y, int z) {
        if (x > min.x() && x < max.x() && y > min.y() && y < max.y() && z > min.z() && z < max.z()) {
            int packed = packCellPos(x, y, z);
            if (getCell(x, y, z, packed)) {
                if (!scanCells(x+1, y, z)) {
                    return false;
                }
                if (!scanCells(x-1, y, z)) {
                    return false;
                }
                if (!scanCells(x, y+1, z)) {
                    return false;
                }
                if (!scanCells(x, y-1, z)) {
                    return false;
                }
                if (!scanCells(x, y, z+1)) {
                    return false;
                }
                if (!scanCells(x, y, z-1)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }
    private static boolean getCell(int x, int y, int z, int packed) {
        if (!currentScan.cells.containsKey(packed)) {
            if (BlockTypes.blockTypes[World.getBlockTypeUnchecked(x, y, z)].permeable()) {
                currentScan.cells.put(packed, null);
                return true;
            }
        }
        return false;
    }
    public static int packCellPos(Vector3i pos) {
        return (pos.x() << 20) | (pos.y() << 10) | pos.z();
    }
    public static int packCellPos(int x, int y, int z) {
        return (x << 20) | (y << 10) | z;
    }
    public static Vector3i unpackCellPos(int xyz) {
        return new Vector3i((xyz >> 20) & 0x3FF, (xyz >> 10) & 0x3FF, xyz & 0x3FF);
    }
}
