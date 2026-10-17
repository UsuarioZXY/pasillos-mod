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
 * Silueta altisima (unos 3,8 bloques): cabeza ovalada grande, cuello fino, torso estrecho,
 * brazos larguisimos extendidos en T y piernas finas. Las medidas estan en 1/16 de bloque y el
 * suelo esta en y = 24.
 */
public class AsechadorModel extends EntityModel<Asechador> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(PasillosMod.MODID, "asechador"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public AsechadorModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // torso (22 de alto) con el cuello encima
        root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 32).addBox(-4.0F, -22.0F, -2.0F, 8.0F, 22.0F, 4.0F)
                        .texOffs(60, 32).addBox(-1.0F, -25.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // cabeza ovalada: caja de 10x10x8 mas una tapa mas estrecha
        root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-5.0F, -10.0F, -4.0F, 10.0F, 10.0F, 8.0F)
                        .texOffs(0, 20).addBox(-4.0F, -12.0F, -3.0F, 8.0F, 2.0F, 6.0F),
                PartPose.offset(0.0F, -25.0F, 0.0F));

        // brazos de 30 de largo, extendidos hacia los lados
        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                        .texOffs(36, 0).addBox(-30.0F, -1.5F, -1.5F, 30.0F, 3.0F, 3.0F),
                PartPose.offset(-4.0F, -20.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                        .texOffs(36, 10).addBox(0.0F, -1.5F, -1.5F, 30.0F, 3.0F, 3.0F),
                PartPose.offset(4.0F, -20.0F, 0.0F));

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
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F);
        this.head.xRot = headPitch * ((float) Math.PI / 180.0F);

        // zancada lenta y rigida
        this.rightLeg.xRot = Mth.cos(limbSwing * 0.5F) * 0.9F * limbSwingAmount;
        this.leftLeg.xRot = -this.rightLeg.xRot;

        // los brazos casi inmoviles, con un balanceo apenas perceptible
        float sway = Mth.sin(ageInTicks * 0.05F) * 0.03F;
        this.rightArm.zRot = sway;
        this.leftArm.zRot = -sway;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
