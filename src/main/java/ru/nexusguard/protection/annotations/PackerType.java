package ru.nexusguard.protection.annotations;

public enum PackerType {
   NONE,
   VIRTUALIZATION,
   MUTATION,
   ULTRA;

   private static PackerType[] $values() {
      return new PackerType[]{NONE, VIRTUALIZATION, MUTATION, ULTRA};
   }
}