package ch.nerdin.minecraft.bungeecord;

import io.fabric8.kubernetes.api.Controller;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Command;

import java.net.InetSocketAddress;
import java.util.logging.Level;

/**
 * Created by edewit on 20/12/16.
 */
public class CreatePodCommand extends Command {

    private static final String MINECRAFT_SERVER_NAME = "minecraft-server";

    public CreatePodCommand() {
        super("openshift", "openshift.pod.create", "opc");
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        createPod(commandSender.getName());
    }

    private void createPod(final String playerName) {
        final String podIP = createPodForPlayer(playerName);

        addToProxy(playerName, podIP);
    }

    private void addToProxy(String playerName, String podIP) {
        final String name = getServerName(playerName);
        final InetSocketAddress inetSocketAddress = new InetSocketAddress(podIP, 25565);
        final String motd = String.format("hello, %s welcome back!", playerName);
        ServerInfo serverInfo = ProxyServer.getInstance().constructServerInfo(name, inetSocketAddress, motd, false);
        ProxyServer.getInstance().getServers().put(name, serverInfo);
    }

    private String createPodForPlayer(String playerName) {
        final String name = getServerName(playerName);
        KubernetesClient client = new DefaultKubernetesClient();
        final PodTemplateSpec template = findMinecraftServerTemplate(client);

        final DoneablePod spec = client.pods().createNew().withNewSpecLike(template.getSpec()).endSpec();
        final Pod pod = spec.withMetadata(new ObjectMetaBuilder().withName(name).build()).done();

        Controller controller = new Controller(client);
        try {
            controller.applyPod(pod, name);
        } catch (Exception e) {
            final String msg = String.format("could not create minecraft instance for player (%s)", name);
            ProxyServer.getInstance().getLogger().log(Level.ALL, msg, e);
        }
        return pod.getStatus().getPodIP();
    }

    private PodTemplateSpec findMinecraftServerTemplate(KubernetesClient kube) {
        final ReplicationController replicationController = kube.replicationControllers().
                withLabel("app", MINECRAFT_SERVER_NAME).list().getItems().get(0);

        return replicationController.getSpec().getTemplate();
    }

    private String getServerName(String playerName) {
        return MINECRAFT_SERVER_NAME + "-" + playerName.toLowerCase();
    }
}
