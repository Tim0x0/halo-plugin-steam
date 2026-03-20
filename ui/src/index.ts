import { definePlugin } from "@halo-dev/ui-shared";

export default definePlugin({
  extensionPoints: {
    "default:editor:extension:create": async () => {
      const { SteamGameCardExtension } = await import("./editor");
      return [SteamGameCardExtension];
    },
  },
});
