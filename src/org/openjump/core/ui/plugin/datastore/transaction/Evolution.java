package org.openjump.core.ui.plugin.datastore.transaction;

import java.util.Arrays;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.Logger;

/**
 * A Feature Evolution. It keeps a clone of the previous state of the feature
 * ( null for a creation) and the new state of the feature (null for a suppression).
 */
public class Evolution {


    private final static String KEY = Evolution.class.getName();

    public enum Type {CREATION, MODIFICATION, SUPPRESSION}

    private Type type;
    // Set<Integer> modifications;
    // newFeature is null for a suppression
    private Feature newFeature;
    // oldFeature contains old values used for conflict resolution
    // It is null for a creation
    private Feature oldFeature;

    private Evolution(Type type, Feature feature, Feature old) {
        assert type.equals(Type.CREATION) && oldFeature == null && feature != null ||
               type.equals(Type.SUPPRESSION) && oldFeature != null && feature == null ||
               type.equals(Type.SUPPRESSION) && oldFeature != null && feature != null;
        this.type = type;
        this.newFeature = feature;
        this.oldFeature = old;
    }

    public Feature getOldFeature() {
        return oldFeature;
    }

    public Feature getNewFeature() {
        return newFeature;
    }

    public Type getType() {
        return type;
    }


    public static Evolution createCreation(Feature feature) {
        return new Evolution(Type.CREATION, feature, null);
    }

    public static Evolution createModification(Feature feature, Feature old /*Set<Integer> modifications*/) {
        return new Evolution(Type.MODIFICATION, feature, old);
    }

    public static Evolution createSuppression(Feature feature) {
        return new Evolution(Type.SUPPRESSION, null, feature);
    }

    public Evolution mergeToPrevious(Evolution previous) throws EvolutionOperationException {

        if (previous == null) return this;

        Feature evo1Old = previous.getOldFeature();
        Feature evo1New = previous.getNewFeature();
        Evolution.Type type1 = previous.getType();

        Feature evo2Old = this.getOldFeature();
        Feature evo2New = this.getNewFeature();
        Evolution.Type type2 = this.getType();

        // Before merging two evolutions, check that the final state of the first one
        // has the same attributes as the initial state of the second
        if (type1 != Type.SUPPRESSION && type2 != Type.CREATION &&
                !Arrays.equals(evo1New.getAttributes(), evo2Old.getAttributes())) {
            Logger.info("Try to merge evolution  : " + previous);
            Logger.info(" which final state is   : " + Arrays.toString(evo1New.getAttributes()));
            Logger.info("        with evolution  : " + this);
            Logger.info(" which initial state is : " + Arrays.toString(evo2Old.getAttributes()));
            throw new EvolutionOperationException(I18N.get(KEY + ".cannot-merge-non-consecutive-evolutions"));
        }

        // previous evolution was a CREATION
        if (type1 == Type.CREATION) {
            switch (type2) {
                case CREATION : throw new EvolutionOperationException(I18N.get(KEY + ".cannot-merge-2-creations") +
                        " (" + previous.getNewFeature().getID() + " - " + getNewFeature().getID() + ")");
                case MODIFICATION : return Evolution.createCreation(evo2New);
                case SUPPRESSION : return null;
                default : return null;
            }
        }
        // previous evolution was a MODIFICATION
        else if (type1 == Type.MODIFICATION) {
            switch (type2) {
                case CREATION : throw new EvolutionOperationException(I18N.get(KEY + ".cannot-merge-a-modification-with-a-creation"));
                case MODIFICATION :
                    boolean unchanged = Arrays.equals(evo1Old.getAttributes(), evo2New.getAttributes());
                    return unchanged ? null : Evolution.createModification(evo2New, evo1Old);
                case SUPPRESSION : return Evolution.createSuppression(evo1Old);
                default : return null;
            }
        }
        // previous evolution was a SUPPRESSION
        else if (type1 == Type.SUPPRESSION) {
            switch (type2) {
                // This can happen with the undo/redo command
                case CREATION :
                    Logger.debug("State before deletion " + Arrays.toString(evo1Old.getAttributes()));
                    Logger.debug("State after creation  " + Arrays.toString(evo2New.getAttributes()));
                    boolean unchanged = Arrays.equals(evo1Old.getAttributes(), evo2New.getAttributes());
                    return unchanged ? null : Evolution.createModification(evo2New, evo1Old);
                case MODIFICATION : throw new EvolutionOperationException(I18N.get(KEY + ".cannot-merge-a-suppression-with-a-modification"));
                case SUPPRESSION : throw new EvolutionOperationException(I18N.get(KEY + ".cannot-merge-a-suppression-with-a-suppression"));
                default : return null;
            }
        }
        return null;
    }

    public String toString() {
        if (type == Type.CREATION) return "Creation: fid=" + newFeature.getID();
        if (type == Type.SUPPRESSION) return "Suppression: fid=" + oldFeature.getID();
        else return "Change from: fid=" + oldFeature.getID() + "to: fid=" + newFeature.getID();
    }

}
