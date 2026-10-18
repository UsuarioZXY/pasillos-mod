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
 * Silueta altisima (unos 3,9 bloques) y descarnada, sin cuernos: cuello largo que se estira hacia
 * delante, craneo ovalado ladeado, mandibula que cuelga con colmillos, torso estrecho con columna y
 * costillas, jirones de tela rota en la espalda y brazos que cuelgan hasta casi tocar el suelo.
 * Los brazos tienen hombro, codo y muneca, y cada mano tiene tres dedos de hueso que se curvan por
 * separado. Las piernas tienen rodilla.
 *
 * Medidas en 1/16 de bloque; la cadera esta en y = 0 y los pies en y = 24. Todo cuelga de una jerarquia
 * (torso > cuello > cabeza > mandibula; torso > brazo > antebrazo > mano > dedos), asi que al inclinar el
 * torso todo se mueve junto. La textura es de 128x128 (ver gen_round5.py: PARTS).
 *
 * Animaciones (todas se mezclan segun cuanto vale cada pose, de 0 a 1, asi no hay saltos):
 *  - Reposo: respiracion, balanceo lento, brazos como pendulos con retraso, dedos que se mueven solos,
 *    tics bruscos de la cabeza y mandibula que se entreabre y tiembla.
 *  - Caminar: zancada con rodilla y cadera torcida, a tirones; brazos con codo; jirones que ondean.
 *  - Agazaparse: cadera baja, muslos adelante y piernas dobladas, torso inclinado, manos en el suelo.
 *  - Asomarse: torso ladeado, cuello estirado por un costado, una mano agarrando la cobertura.
 *  - Pillado (STARE): se endereza de golpe, boca muy abierta, manos abiertas y todo el cuerpo tiembla.
 */
public class AsechadorModel extends EntityModel<Asechador> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(PasillosMod.MODID, "asechador"), "main");

    private static final float DEG = (float) Math.PI / 180.0F;

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart ragLeft;
    private final ModelPart ragRight;
    private final ModelPart[] arm = new ModelPart[2];
    private final ModelPart[] forearm = new ModelPart[2];
    private final ModelPart[] hand = new ModelPart[2];
    private final ModelPart[][] finger = new ModelPart[2][3];
    private final ModelPart[] leg = new ModelPart[2];
    private final ModelPart[] shin = new ModelPart[2];

    public AsechadorModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.neck = this.body.getChild("neck");
        this.head = this.neck.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.ragLeft = this.body.getChild("rag_left");
        this.ragRight = this.body.getChild("rag_right");
        for (int i = 0; i < 2; i++) {
            String side = i == 0 ? "right" : "left";
            this.arm[i] = this.body.getChild(side + "_arm");
            this.forearm[i] = this.arm[i].getChild(side + "_forearm");
            this.hand[i] = this.forearm[i].getChild(side + "_hand");
            for (int j = 0; j < 3; j++) {
                this.finger[i][j] = this.hand[i].getChild(side + "_finger_" + j);
            }
            this.leg[i] = root.getChild(side + "_leg");
            this.shin[i] = this.leg[i].getChild(side + "_shin");
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // torso estrecho de 21 de alto con tres vertebras que sobresalen en la espalda; pivote en la cadera
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 24).addBox(-4.0F, -21.0F, -2.0F, 8.0F, 21.0F, 4.0F)
                        .texOffs(112, 0).addBox(-1.0F, -18.0F, 2.0F, 2.0F, 3.0F, 2.0F)
                        .texOffs(112, 0).addBox(-1.0F, -12.0F, 2.0F, 2.0F, 3.0F, 2.0F)
                        .texOffs(112, 0).addBox(-1.0F, -6.0F, 2.0F, 2.0F, 3.0F, 2.0F),
                PartPose.ZERO);

        // cuello largo (pivote en lo alto del torso) y encima la cabeza
        PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create()
                        .texOffs(88, 0).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offset(0.0F, -21.0F, 0.0F));

        // craneo ovalado: caja de 10x7x8 mas una tapa mas estrecha
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-5.0F, -10.0F, -4.0F, 10.0F, 7.0F, 8.0F)
                        .texOffs(36, 0).addBox(-4.0F, -12.0F, -3.0F, 8.0F, 2.0F, 6.0F),
                PartPose.offset(0.0F, -6.0F, 0.0F));

        // mandibula: bisagra atras, cuelga y se abre
        head.addOrReplaceChild("jaw", CubeListBuilder.create()
                        .texOffs(64, 0).addBox(-3.0F, 0.0F, -7.0F, 6.0F, 4.0F, 6.0F),
                PartPose.offset(0.0F, -3.0F, 3.0F));

        // jirones de tela rota que cuelgan de los hombros por la espalda
        body.addOrReplaceChild("rag_left", CubeListBuilder.create()
                        .texOffs(120, 0).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 20.0F, 1.0F),
                PartPose.offset(2.6F, -19.0F, 2.3F));
        body.addOrReplaceChild("rag_right", CubeListBuilder.create()
                        .texOffs(120, 0).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 20.0F, 1.0F),
                PartPose.offset(-2.6F, -19.0F, 2.3F));

        // brazos: hombro (15) > codo (16) > muneca (2) > tres dedos de 8; en total 41, casi hasta el suelo
        for (int i = 0; i < 2; i++) {
            String side = i == 0 ? "right" : "left";
            float shoulderX = i == 0 ? -5.5F : 5.5F;

            PartDefinition upper = body.addOrReplaceChild(side + "_arm", CubeListBuilder.create()
                            .texOffs(24, 24).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 15.0F, 3.0F),
                    PartPose.offset(shoulderX, -19.0F, 0.0F));
            PartDefinition fore = upper.addOrReplaceChild(side + "_forearm", CubeListBuilder.create()
                            .texOffs(36, 24).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 16.0F, 3.0F),
                    PartPose.offset(0.0F, 15.0F, 0.0F));
            PartDefinition palm = fore.addOrReplaceChild(side + "_hand", CubeListBuilder.create()
                            .texOffs(100, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 2.0F, 3.0F),
                    PartPose.offset(0.0F, 16.0F, 0.0F));
            for (int j = 0; j < 3; j++) {
                palm.addOrReplaceChild(side + "_finger_" + j, CubeListBuilder.create()
                                .texOffs(96, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 8.0F, 1.0F),
                        PartPose.offset(-1.0F + j, 2.0F, -1.0F + j));
            }
        }

        // piernas: muslo (12) y espinilla (11) con un pie plano de 1; los pies llegan a y = 24
        for (int i = 0; i < 2; i++) {
            String side = i == 0 ? "right" : "left";
            float hipX = i == 0 ? -2.5F : 2.5F;

            PartDefinition thigh = root.addOrReplaceChild(side + "_leg", CubeListBuilder.create()
                            .texOffs(48, 24).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 12.0F, 3.0F),
                    PartPose.offset(hipX, 0.0F, 0.0F));
            thigh.addOrReplaceChild(side + "_shin", CubeListBuilder.create()
                            .texOffs(60, 24).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 11.0F, 3.0F)
                            .texOffs(72, 24).addBox(-1.5F, 11.0F, -3.5F, 3.0F, 1.0F, 5.0F),
                    PartPose.offset(0.0F, 12.0F, 0.0F));
        }

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(Asechador entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        float pt = ageInTicks - entity.tickCount;
        float crouch = entity.getCrouchAmount(pt);
        float peek = entity.getPeekAmount(pt);
        float stare = entity.getStareAmount(pt);

        // cada Asechador tiene su propio ritmo y se asoma por su propio lado
        float side = (entity.getId() & 1) == 0 ? 1.0F : -1.0F;
        float t = ageInTicks + (entity.getId() % 97) * 13.0F;
        float walk = Math.min(1.0F, limbSwingAmount * 1.5F);

        float breathe = Mth.sin(t * 0.09F);
        float sway = Mth.sin(t * 0.04F);
        // tic: de vez en cuando la cabeza se ladea de golpe, a un lado o a otro
        float snap = Math.max(0.0F, Mth.sin(t * 0.13F) * Mth.sin(t * 0.47F) - 0.55F) * 1.8F;
        float snapDir = Mth.sin(t * 0.031F) > 0.0F ? 1.0F : -1.0F;
        // fase de la zancada, a tirones: la velocidad cambia de un paso a otro
        float p = limbSwing * 0.5F + 0.35F * Mth.sin(limbSwing);
        float sp = Mth.sin(p);
        float cp = Mth.cos(p);
        float jitter = stare * (Mth.sin(t * 2.7F) * 0.014F + Mth.sin(t * 4.3F) * 0.01F);

        // ---------------------------------------------------------------- torso y piernas
        float a1 = -1.0F * crouch;      // muslo hacia delante
        float a2 = 1.25F * crouch;      // rodilla hacia atras
        // al doblar las piernas la cadera baja; esto lo compensa para que los pies sigan en el suelo
        float drop = 24.0F - (12.0F * Mth.cos(a1) + 12.0F * Mth.cos(a1 + a2));

        float lean = 0.85F * crouch + 0.12F * walk * (1.0F - crouch) + 0.10F * peek;
        this.body.y = drop;
        this.body.xRot = lean + breathe * 0.012F - stare * 0.05F;
        this.body.yRot = sp * 0.09F * walk;
        this.body.zRot = side * 0.32F * peek + sway * 0.025F * (1.0F - peek) + cp * 0.03F * walk + jitter;

        float wk = walk * (1.0F - 0.55F * crouch);
        this.leg[0].y = drop;
        this.leg[1].y = drop;
        this.leg[0].xRot = a1 + sp * 0.85F * wk;
        this.leg[1].xRot = a1 - sp * 0.85F * wk;
        this.leg[0].zRot = 0.12F * crouch;
        this.leg[1].zRot = -0.12F * crouch;
        this.shin[0].xRot = a2 + Math.max(0.0F, -cp) * wk;
        this.shin[1].xRot = a2 + Math.max(0.0F, cp) * wk;

        // ---------------------------------------------------------------- cuello y cabeza
        this.neck.xRot = 0.20F + 0.06F * breathe - 0.60F * crouch + 0.30F * peek - 0.28F * stare;
        this.neck.yRot = 0.0F;
        this.neck.zRot = side * 0.35F * peek + sway * 0.03F;

        this.head.yRot = netHeadYaw * DEG + stare * Mth.sin(t * 3.1F) * 0.03F;
        this.head.xRot = headPitch * DEG - lean * 0.3F + stare * Mth.sin(t * 3.7F) * 0.03F;
        this.head.zRot = (0.10F + snap * snapDir) * (1.0F - stare) - side * 0.55F * peek;

        this.jaw.xRot = 0.10F
                + Math.max(0.0F, Mth.sin(t * 0.21F)) * 0.08F
                + snap * 0.35F
                + peek * (0.35F + Mth.sin(t * 1.8F) * 0.03F)
                + crouch * 0.20F
                + stare * (0.75F + Mth.sin(t * 2.9F) * 0.06F);

        // ---------------------------------------------------------------- jirones de tela
        float flutter = limbSwingAmount * 0.45F + crouch * 0.5F;
        this.ragLeft.xRot = 0.06F + flutter + Mth.sin(t * 0.07F) * 0.07F;
        this.ragRight.xRot = 0.06F + flutter + Mth.sin(t * 0.07F + 1.3F) * 0.07F;
        this.ragLeft.zRot = Mth.sin(t * 0.05F + 0.7F) * 0.05F;
        this.ragRight.zRot = Mth.sin(t * 0.05F + 2.1F) * 0.05F;

        // ---------------------------------------------------------------- brazos y dedos
        // el brazo del lado hacia el que se inclina es el que agarra la cobertura
        int grabArm = side > 0.0F ? 1 : 0;
        for (int i = 0; i < 2; i++) {
            float out = i == 0 ? 1.0F : -1.0F;     // hacia fuera: zRot positivo a la derecha, negativo a la izquierda
            float swing = (i == 0 ? -1.0F : 1.0F) * sp * 0.38F * walk;
            float lag = Mth.sin(t * 0.05F + i * 1.7F);

            float upperX = swing + lag * 0.035F + breathe * 0.02F;
            float upperZ = out * (0.07F + breathe * 0.02F + 0.22F * stare);
            float foreX = -0.12F - Math.max(0.0F, -swing) * 1.1F + Mth.sin(t * 0.05F + i * 1.7F - 0.6F) * 0.06F;

            // agazapado: los brazos bajan por delante hasta apoyar las manos
            upperX = Mth.lerp(crouch, upperX, -1.40F);
            foreX = Mth.lerp(crouch, foreX, -0.27F);
            upperZ = Mth.lerp(crouch, upperZ, out * 0.12F);

            // pillado: brazos algo separados y codos doblados
            upperX = Mth.lerp(stare, upperX, -0.15F);
            foreX = Mth.lerp(stare, foreX, -0.45F);

            // asomado: el brazo del lado de la inclinacion sube y se agarra
            if (i == grabArm) {
                upperX = Mth.lerp(peek, upperX, -1.25F);
                upperZ = Mth.lerp(peek, upperZ, out * 0.9F);
                foreX = Mth.lerp(peek, foreX, -0.75F);
            }

            this.arm[i].xRot = upperX;
            this.arm[i].zRot = upperZ;
            this.arm[i].yRot = 0.0F;
            this.forearm[i].xRot = foreX;
            this.hand[i].xRot = -0.1F + Mth.sin(t * 0.08F + i * 2.3F) * 0.06F;

            for (int j = 0; j < 3; j++) {
                // dedos que se mueven solos y, a veces, un espasmo
                float curl = 0.15F + 0.12F * Mth.sin(t * 0.11F + j * 1.9F + i * 0.8F);
                float spasm = Math.max(0.0F, Mth.sin(t * 0.23F + j * 2.1F + i) * Mth.sin(t * 0.61F + j) - 0.6F) * 1.2F;
                curl += spasm;
                curl = Mth.lerp(crouch, curl, 0.25F);                         // dedos casi rectos al apoyarse
                curl = Mth.lerp(stare, curl, -0.2F + Mth.sin(t * 3.3F + j) * 0.12F);   // abiertos y temblando
                if (i == grabArm) {
                    curl = Mth.lerp(peek, curl, 1.25F);                       // agarrando
                }
                this.finger[i][j].xRot = curl;
                this.finger[i][j].zRot = (j - 1) * 0.10F * (1.0F + 2.5F * stare);
            }
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
