import {
  mergeAttributes,
  Node,
  ToolboxItem,
  VueNodeViewRenderer,
  type Editor,
  type Range,
  type ExtensionOptions,
} from "@halo-dev/richtext-editor";
import { markRaw } from "vue";
import MdiSteam from "~icons/mdi/steam";
import SteamGameCardView from "../components/SteamGameCardView.vue";

export const SteamGameCardExtension = Node.create<ExtensionOptions>({
  name: "steamGameCard",
  atom: true,
  group: "block",
  draggable: true,

  addAttributes() {
    return {
      appId: {
        default: null,
        parseHTML: (element: HTMLElement) => element.getAttribute("app-id"),
        renderHTML: (attributes: Record<string, any>) => {
          if (!attributes.appId) return {};
          return { "app-id": attributes.appId };
        },
      },
      theme: {
        default: "steam-dark",
        parseHTML: (element: HTMLElement) =>
          element.getAttribute("theme") || "steam-dark",
      },
      showDescription: {
        default: true,
        parseHTML: (element: HTMLElement) =>
          element.getAttribute("show-description") !== "false",
        renderHTML: (attributes: Record<string, any>) => ({
          "show-description": String(attributes.showDescription),
        }),
      },
      showPlaytime: {
        default: true,
        parseHTML: (element: HTMLElement) =>
          element.getAttribute("show-playtime") !== "false",
        renderHTML: (attributes: Record<string, any>) => ({
          "show-playtime": String(attributes.showPlaytime),
        }),
      },
      showAchievements: {
        default: true,
        parseHTML: (element: HTMLElement) =>
          element.getAttribute("show-achievements") !== "false",
        renderHTML: (attributes: Record<string, any>) => ({
          "show-achievements": String(attributes.showAchievements),
        }),
      },
      showLastPlayed: {
        default: true,
        parseHTML: (element: HTMLElement) =>
          element.getAttribute("show-last-played") !== "false",
        renderHTML: (attributes: Record<string, any>) => ({
          "show-last-played": String(attributes.showLastPlayed),
        }),
      },
      showPrice: {
        default: true,
        parseHTML: (element: HTMLElement) =>
          element.getAttribute("show-price") !== "false",
        renderHTML: (attributes: Record<string, any>) => ({
          "show-price": String(attributes.showPrice),
        }),
      },
      showDeveloper: {
        default: true,
        parseHTML: (element: HTMLElement) =>
          element.getAttribute("show-developer") !== "false",
        renderHTML: (attributes: Record<string, any>) => ({
          "show-developer": String(attributes.showDeveloper),
        }),
      },
    };
  },

  addOptions() {
    return {
      ...this.parent?.(),
      getCommandMenuItems() {
        return {
          priority: 100,
          icon: markRaw(MdiSteam),
          title: "Steam 游戏卡片",
          keywords: ["steam", "game", "card", "游戏"],
          command: ({ editor, range }: { editor: Editor; range: Range }) => {
            editor
              .chain()
              .deleteRange(range)
              .focus()
              .insertContent([{ type: "steamGameCard", attrs: {} }])
              .run();
          },
        };
      },
      getToolboxItems({ editor }: { editor: Editor }) {
        return [
          {
            priority: 60,
            component: markRaw(ToolboxItem),
            props: {
              editor,
              icon: markRaw(MdiSteam),
              title: "Steam 游戏卡片",
              action: () => {
                editor
                  .chain()
                  .focus()
                  .insertContent([{ type: "steamGameCard", attrs: {} }])
                  .run();
              },
            },
          },
        ];
      },
    };
  },

  parseHTML() {
    return [{ tag: "steam-game-card" }];
  },

  renderHTML({ HTMLAttributes }) {
    return ["steam-game-card", mergeAttributes(HTMLAttributes)];
  },

  addNodeView() {
    return VueNodeViewRenderer(SteamGameCardView);
  },
});
