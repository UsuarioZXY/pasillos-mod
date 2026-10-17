package com.pasillos.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pasillos.PasillosMod;
import com.pasillos.entity.Asechador;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Silueta altisima (unos 3,8 bloques) y descarnada: cuello largo, cabeza ovalada ladeada con dos
 * cuernos finos, mandibula que cuelga con dientes, torso estrecho con la columna marcada, brazos que
 * cuelgan hasta las rodillas y terminan en dedos larguisimos de hueso. Las medidas estan en 1/16 de
 * bloque y el suelo esta en y = 24. La cabeza y los brazos cuelgan del torso, asi que al inclinarlo
 * (agazaparse, asomarse) todo se mueve junto.
 *
 * Animaciones: respiracion y balanceo lento, tics bruscos de la cabeza, la mandibula que se abre y
 * cierra, zancada rigida, agazapado y asomarse con una mano apoyada.
 */
public class AsechadorModel extends EntityModel<Asechador> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(PasillosMod.MODID, "asechador"), "main");

    private static final float DEG = (float) Math.PI / 180.0F;
    private static final float CROUCH_LEAN = 0.85F;

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public AsechadorModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.rightArm = this.body.getChild("right_arm");
        this.leftArm = this.body.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // torso estrecho de 22 de alto, cuello largo y tres vertebras que sobresalen en la espalda
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 32).addBox(-4.0F, -22.0F, -2.0F, 8.0F, 22.0F, 4.0F)
                        .texOffs(60, 32).addBox(-1.0F, -26.0F, -1.0F, 2.0F, 4.0F, 2.0F)
                        .texOffs(96, 40).addBox(-1.0F, -19.0F, 2.0F, 2.0F, 3.0F, 2.0F)
                        .texOffs(96, 40).addBox(-1.0F, -13.0F, 2.0F, 2.0F, 3.0F, 2.0F)
                        .texOffs(96, 40).addBox(-1.0F, -7.0F, 2.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // craneo ovalado: caja de 10x7x8 mas una tapa mas estrecha
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-5.0F, -10.0F, -4.0F, 10.0F, 7.0F, 8.0F)
                        .texOffs(0, 20).addBox(-4.0F, -12.0F, -3.0F, 8.0F, 2.0F, 6.0F),
                PartPose.offset(0.0F, -26.0F, 0.0F));

        // mandibula: bisagra en la parte de atras, cuelga y se abre
        head.addOrReplaceChild("jaw", CubeListBuilder.create()
                        .texOffs(80, 40).addBox(-3.0F, 0.0F, -7.0F, 6.0F, 4.0F, 6.0F),
                PartPose.offset(0.0F, -3.0F, 3.0F));

        // dos cuernos finos que se abren hacia fuera
        head.addOrReplaceChild("horn_left", CubeListBuilder.create()
                        .texOffs(100, 20).addBox(-0.5F, -7.0F, -0.5F, 1.0F, 7.0F, 1.0F),
                PartPose.offsetAndRotation(2.5F, -11.0F, 0.0F, 0.0F, 0.0F, 0.32F));
        head.addOrReplaceChild("horn_right", CubeListBuilder.create()
                        .texOffs(100, 20).addBox(-0.5F, -7.0F, -0.5F, 1.0F, 7.0F, 1.0F),
                PartPose.offsetAndRotation(-2.5F, -11.0F, 0.0F, 0.0F, 0.0F, -0.32F));

        // brazos finisimos de 34 de largo que cuelgan hasta las rodillas, con tres dedos de hueso
        body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                        .texOffs(36, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 34.0F, 3.0F)
                        .texOffs(96, 0).addBox(-1.5F, 34.0F, -1.2F, 1.0F, 7.0F, 1.0F)
                        .texOffs(96, 0).addBox(-0.5F, 34.0F, -0.5F, 1.0F, 8.0F, 1.0F)
                        .texOffs(96, 0).addBox(0.5F, 34.0F, 0.2F, 1.0F, 7.0F, 1.0F),
                PartPose.offset(-5.5F, -20.0F, 0.0F));
        body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                        .texOffs(52, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 34.0F, 3.0F)
                        .texOffs(96, 0).addBox(-1.5F, 34.0F, -1.2F, 1.0F, 7.0F, 1.0F)
                        .texOffs(96, 0).addBox(-0.5F, 34.0F, -0.5F, 1.0F, 8.0F, 1.0F)
                        .texOffs(96, 0).addBox(0.5F, 34.0F, 0.2F, 1.0F, 7.0F, 1.0F),
                PartPose.offset(5.5F, -20.0F, 0.0F));

        // piernas finas de 24 de largo
        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                        .texOffs(24, 32).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 24.0F, 3.0F),
                PartPose.offset(-2.5F, 0.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                        .texOffs(40, 32).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 24.0F, 3.0F),
                PartPose.offset(2.5F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(Asechador entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        int state = entity.getState();
        boolean crouch = state == Asechador.STATE_CROUCH;
        boolean peek = state == Asechador.STATE_PEEK;

        float breathe = Mth.sin(ageInTicks * 0.09F);
        // tic: de vez en cuando la cabeza se ladea de golpe
        float twitch = Math.max(0.0F, Mth.sin(ageInTicks * 0.13F) * Mth.sin(ageInTicks * 0.47F) - 0.55F) * 1.8F;

        // --- torso y piernas ---
        float lean = crouch ? CROUCH_LEAN : 0.0F;
        float sideLean = peek ? 0.32F : 0.0F;
        // al doblar las piernas la cadera baja; esto compensa para que los pies sigan en el suelo
        float drop = crouch ? 24.0F * (1.0F - Mth.cos(lean)) : 0.0F;

        this.body.xRot = lean;
        this.body.zRot = sideLean;
        this.body.y = drop;
        this.rightLeg.y = drop;
        this.leftLeg.y = drop;

        float stride = Mth.cos(limbSwing * 0.45F) * limbSwingAmount;
        float legBase = crouch ? -lean : 0.0F;
        float legSwing = crouch ? 0.4F : 1.0F;
        this.rightLeg.xRot = legBase + stride * legSwing;
        this.leftLeg.xRot = legBase - stride * legSwing;

        // --- cabeza: sigue al jugador, se inclina un poco de mas y tiene tics ---
        this.head.yRot = netHeadYaw * DEG;
        this.head.xRot = headPitch * DEG - lean * 0.6F;
        this.head.zRot = 0.1F + twitch - sideLean * 0.8F;

        // --- mandibula: entreabierta, con pulsos lentos; se abre del todo en los tics y al asomarse ---
        this.jaw.xRot = 0.12F
                + Math.max(0.0F, Mth.sin(ageInTicks * 0.21F)) * 0.08F
                + twitch * 0.35F
                + (peek ? 0.45F : 0.0F)
                + (crouch ? 0.25F : 0.0F);

        // --- brazos: cuelgan, se balancean apenas; agazapado apoya las manos delante; asomado agarra ---
        this.rightArm.xRot = -stride * 0.5F + breathe * 0.03F;
        this.leftArm.xRot = stride * 0.5F - breathe * 0.03F;
        this.rightArm.zRot = 0.05F + breathe * 0.02F;
        this.leftArm.zRot = -0.05F - breathe * 0.02F;

        if (crouch) {
            this.rightArm.xRot = -0.9F + breathe * 0.04F;
            this.leftArm.xRot = -0.9F - breathe * 0.04F;
        }
        if (peek) {
            this.rightArm.xRot = -1.45F;
            this.rightArm.zRot = 0.25F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
